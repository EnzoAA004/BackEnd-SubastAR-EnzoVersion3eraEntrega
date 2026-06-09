package com.subastar.subastar.service;

import com.subastar.subastar.dto.puja.PujaRequest;
import com.subastar.subastar.dto.puja.PujaResumen;
import com.subastar.subastar.exception.BadRequestException;
import com.subastar.subastar.exception.ForbiddenException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PujaService {

    private final SubastaRepository subastaRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final SubastaExtraRepository subastaExtraRepository;
    private final AsistenteRepository asistenteRepository;
    private final PujoRepository pujoRepository;
    private final PujoExtraRepository pujoExtraRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final ChequeCertificadoRepository chequeCertificadoRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;
    private final MultaRepository multaRepository;
    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public PujaResumen pujar(Integer subastaId, PujaRequest req, String email) {
        Credencial cred = credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Cliente cliente = clienteRepository.findById(cred.getPersonaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));

        if (!"abierta".equals(subasta.getEstado())) {
            throw new BadRequestException("La subasta no está en vivo");
        }

        // A-10: bloquear usuarios no admitidos o con multas
        if (!"si".equals(cliente.getAdmitido())) {
            throw new ForbiddenException("Tu cuenta no está habilitada para pujar");
        }

        BigDecimal multaPendiente = multaRepository.sumMultasPendientesByClienteId(cliente.getIdentificador());
        if (multaPendiente != null && multaPendiente.compareTo(BigDecimal.ZERO) > 0) {
            throw new ForbiddenException("No podés pujar mientras tengas multas pendientes ($" + multaPendiente + ")");
        }

        // A-8: validar categoría del cliente vs categoría de la subasta
        if (!categoriaHabilita(cliente.getCategoria(), subasta.getCategoria())) {
            throw new ForbiddenException("Tu categoría '" + cliente.getCategoria()
                    + "' no te permite participar en subastas de categoría '" + subasta.getCategoria() + "'");
        }

        // Obtener el ítem actual con lock pesimista para evitar race condition
        SubastaExtra extra = subastaExtraRepository.findBySubastaIdWithLock(subastaId)
                .orElseThrow(() -> new BadRequestException("No hay ítem activo en esta subasta"));

        if (extra.getItemActualId() == null) {
            throw new BadRequestException("No hay ítem activo en esta subasta");
        }

        ItemCatalogo item = itemCatalogoRepository.findById(extra.getItemActualId())
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));

        // A-9: validar medio de pago: que pertenezca al usuario Y esté verificado
        MedioPago medioPago = medioPagoRepository
                .findByIdAndClienteIdentificadorAndEliminadoFalse(req.getMedioPagoId(), cliente.getIdentificador())
                .orElseThrow(() -> new ForbiddenException("El medio de pago no pertenece al usuario"));

        if (!medioPago.isVerificado()) {
            throw new ForbiddenException("El medio de pago no está verificado por la empresa");
        }

        // M-9: validar compatibilidad de moneda entre subasta y medio de pago
        if ("USD".equals(extra.getMoneda())) {
            if ("cheque_certificado".equals(medioPago.getTipo())) {
                throw new ForbiddenException("Los cheques certificados no son válidos para subastas en dólares.");
            }
            if ("cuenta_bancaria".equals(medioPago.getTipo())) {
                cuentaBancariaRepository.findByMedioPagoId(medioPago.getId()).ifPresent(cb -> {
                    if ("Argentina".equalsIgnoreCase(cb.getPaisBanco())) {
                        throw new ForbiddenException("Para subastas en dólares necesitás una cuenta bancaria de un banco extranjero.");
                    }
                });
            }
            if ("tarjeta_credito".equals(medioPago.getTipo())) {
                tarjetaCreditoRepository.findByMedioPagoId(medioPago.getId()).ifPresent(tc -> {
                    if (!tc.isEsInternacional()) {
                        throw new ForbiddenException("Para subastas en dólares necesitás una tarjeta de crédito internacional.");
                    }
                });
            }
        }

        // A-11: validar que la puja no supere el límite del medio de pago
        validarLimiteMedioPago(medioPago, req.getMonto());

        // Validar monto
        BigDecimal precioBase = item.getPrecioBase();
        BigDecimal mejorOferta = pujoRepository.findTopByItemIdentificadorOrderByImporteDesc(item.getIdentificador())
                .map(Pujo::getImporte).orElse(BigDecimal.ZERO);

        boolean esOroOPlatino = "oro".equals(cliente.getCategoria()) || "platino".equals(cliente.getCategoria());

        BigDecimal minimo = calcularPujaMinima(precioBase, mejorOferta);

        if (req.getMonto().compareTo(minimo) < 0) {
            throw new BadRequestException("La puja mínima para este lote es " + minimo);
        }

        if (!esOroOPlatino) {
            BigDecimal maximo = calcularPujaMaxima(precioBase, mejorOferta);
            if (req.getMonto().compareTo(maximo) > 0) {
                throw new BadRequestException("La puja máxima permitida para tu categoría es " + maximo);
            }
        }

        // Validar que no esté en otra subasta abierta
        if (asistenteRepository.existsEnOtraSubastaAbierta(cliente.getIdentificador(), subastaId)) {
            throw new com.subastar.subastar.exception.ForbiddenException("Ya estás conectado a otra subasta. No podés participar en más de una a la vez.");
        }

        // Obtener o crear asistente
        Asistente asistente = asistenteRepository
                .findByClienteIdentificadorAndSubastaIdentificador(cliente.getIdentificador(), subastaId)
                .orElseGet(() -> {
                    int maxPostor = asistenteRepository.findMaxNumeroPostorBySubastaId(subastaId);
                    Asistente nuevo = new Asistente();
                    nuevo.setCliente(cliente);
                    nuevo.setSubasta(subasta);
                    nuevo.setNumeroPostor(maxPostor + 1);
                    return asistenteRepository.save(nuevo);
                });

        // Registrar puja
        Pujo pujo = new Pujo();
        pujo.setAsistente(asistente);
        pujo.setItem(item);
        pujo.setImporte(req.getMonto());
        pujo.setGanador("no");
        pujo = pujoRepository.save(pujo);

        PujoExtra pe = new PujoExtra();
        pe.setPujoId(pujo.getIdentificador());
        pe.setMedioPagoId(req.getMedioPagoId());
        pujoExtraRepository.save(pe);

        String nombreItem = item.getProducto().getDescripcionCatalogo() != null
                ? item.getProducto().getDescripcionCatalogo() : "Ítem #" + item.getIdentificador();
        notificacionService.notificarPujaRegistrada(cliente, nombreItem, req.getMonto());

        return toPujaResumen(pujo, pe);
    }

    public List<PujaResumen> getHistorialPujas(Integer subastaId, Integer itemId) {
        subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        return pujoRepository.findByItemIdentificadorOrderByIdentificadorDesc(itemId)
                .stream().map(p -> {
                    PujoExtra pe = pujoExtraRepository.findByPujoId(p.getIdentificador()).orElse(null);
                    return toPujaResumen(p, pe);
                }).collect(Collectors.toList());
    }

    private PujaResumen toPujaResumen(Pujo p, PujoExtra pe) {
        PujaResumen r = new PujaResumen();
        r.setId(p.getIdentificador());
        r.setUsuarioId(p.getAsistente().getCliente().getIdentificador());
        Persona persona = p.getAsistente().getCliente().getPersona();
        r.setNombreUsuario(persona != null ? persona.getNombre() : "Usuario " + r.getUsuarioId());
        r.setMonto(p.getImporte());
        if (pe != null) r.setTimestamp(pe.getTimestampPuja());
        r.setEsGanadora("si".equals(p.getGanador()));
        return r;
    }

    // A-8: nivel numérico de categoría para comparación
    private int nivelCategoria(String categoria) {
        if (categoria == null) return 0;
        return switch (categoria.toLowerCase()) {
            case "comun"   -> 1;
            case "especial"-> 2;
            case "plata"   -> 3;
            case "oro"     -> 4;
            case "platino" -> 5;
            default        -> 0;
        };
    }

    private boolean categoriaHabilita(String categoriaCliente, String categoriaSubasta) {
        if (categoriaSubasta == null) return true;
        return nivelCategoria(categoriaCliente) >= nivelCategoria(categoriaSubasta);
    }

    private BigDecimal calcularPujaMinima(BigDecimal precioBase, BigDecimal mejorOferta) {
        if (precioBase == null) return BigDecimal.ZERO;
        if (mejorOferta == null || mejorOferta.compareTo(BigDecimal.ZERO) <= 0) return precioBase;
        return mejorOferta.add(precioBase.multiply(BigDecimal.valueOf(0.01)));
    }

    private BigDecimal calcularPujaMaxima(BigDecimal precioBase, BigDecimal mejorOferta) {
        if (precioBase == null) return BigDecimal.ZERO;
        BigDecimal incrementoMaximo = precioBase.multiply(BigDecimal.valueOf(0.20));
        if (mejorOferta == null || mejorOferta.compareTo(BigDecimal.ZERO) <= 0) {
            return precioBase.add(incrementoMaximo);
        }
        return mejorOferta.add(incrementoMaximo);
    }

    // A-11: validar que la puja no supere el límite del medio de pago
    private void validarLimiteMedioPago(MedioPago medioPago, BigDecimal monto) {
        switch (medioPago.getTipo()) {
            case "cheque_certificado" -> {
                ChequeCertificado cheque = chequeCertificadoRepository
                        .findByMedioPagoId(medioPago.getId())
                        .orElseThrow(() -> new BadRequestException("Cheque certificado no encontrado"));
                if (monto.compareTo(cheque.getMontoCertificado()) > 0) {
                    throw new BadRequestException(
                            "La puja ($" + monto + ") supera el monto certificado disponible ($"
                            + cheque.getMontoCertificado() + ")");
                }
            }
            case "cuenta_bancaria" -> {
                CuentaBancaria cuenta = cuentaBancariaRepository
                        .findByMedioPagoId(medioPago.getId())
                        .orElseThrow(() -> new BadRequestException("Cuenta bancaria no encontrada"));
                if (cuenta.getFondosReservados() != null
                        && cuenta.getFondosReservados().compareTo(BigDecimal.ZERO) > 0
                        && monto.compareTo(cuenta.getFondosReservados()) > 0) {
                    throw new BadRequestException(
                            "La puja ($" + monto + ") supera los fondos reservados ($"
                            + cuenta.getFondosReservados() + ")");
                }
            }
        }
    }
}
