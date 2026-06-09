package com.subastar.subastar.service;

import com.subastar.subastar.dto.medioPago.*;
import com.subastar.subastar.exception.BadRequestException;
import com.subastar.subastar.exception.ConflictException;
import com.subastar.subastar.exception.ForbiddenException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MedioPagoService {

    private final MedioPagoRepository medioPagoRepository;
    private final CuentaBancariaRepository cuentaBancariaRepository;
    private final TarjetaCreditoRepository tarjetaCreditoRepository;
    private final ChequeCertificadoRepository chequeCertificadoRepository;
    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;
    private final PujoRepository pujoRepository;
    private final NotificacionService notificacionService;

    public List<MedioPagoResumen> listar(String email) {
        Integer clienteId = getClienteId(email);
        return medioPagoRepository.findByClienteIdentificadorAndEliminadoFalse(clienteId)
                .stream().map(this::toResumen).collect(Collectors.toList());
    }

    @Transactional
    public MedioPagoResumen agregarCuenta(String email, CuentaBancariaRequest req) {
        Integer clienteId = getClienteId(email);
        // Verificar unicidad del CBU solo dentro de los medios de pago del cliente
        boolean cbuYaRegistrado = medioPagoRepository
                .findByClienteIdentificadorAndEliminadoFalse(clienteId).stream()
                .anyMatch(mp -> cuentaBancariaRepository.findByMedioPagoId(mp.getId())
                        .map(cb -> cb.getCbuIban().equals(req.getCbuIban()))
                        .orElse(false));
        if (cbuYaRegistrado) {
            throw new ConflictException("Ya tenés una cuenta registrada con ese CBU/IBAN");
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        MedioPago mp = new MedioPago();
        mp.setCliente(cliente);
        mp.setTipo("cuenta_bancaria");
        mp.setDescripcion("Cuenta bancaria " + req.getNombreBanco() + " - CBU terminado en "
                + req.getCbuIban().substring(Math.max(0, req.getCbuIban().length() - 4)));
        mp = medioPagoRepository.save(mp);

        CuentaBancaria cb = new CuentaBancaria();
        cb.setMedioPagoId(mp.getId());
        cb.setNombreBanco(req.getNombreBanco());
        cb.setPaisBanco(req.getPaisBanco());
        cb.setCbuIban(req.getCbuIban());
        cb.setFondosReservados(req.getFondosReservados());
        cuentaBancariaRepository.save(cb);

        recalcularCategoria(clienteId);
        notificacionService.notificarMedioPagoAgregado(cliente, mp.getDescripcion());
        return toResumen(mp);
    }

    @Transactional
    public MedioPagoResumen agregarTarjeta(String email, TarjetaCreditoRequest req) {
        Integer clienteId = getClienteId(email);
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        String numeroTarjeta = req.getNumeroTarjeta().trim();
        String masked = "**** **** **** " + numeroTarjeta.substring(Math.max(0, numeroTarjeta.length() - 4));

        MedioPago mp = new MedioPago();
        mp.setCliente(cliente);
        mp.setTipo("tarjeta_credito");
        mp.setDescripcion("Tarjeta de crédito - " + req.getTitular().trim() + " terminada en "
                + numeroTarjeta.substring(Math.max(0, numeroTarjeta.length() - 4)));
        mp = medioPagoRepository.save(mp);

        TarjetaCredito tc = new TarjetaCredito();
        tc.setMedioPagoId(mp.getId());
        tc.setNumeroTarjetaMasked(masked);
        tc.setTitular(req.getTitular());
        tc.setVencimiento(req.getVencimiento());
        tc.setEsInternacional(req.isEsInternacional());
        tc.setDniTitular(req.getDniTitular());
        tarjetaCreditoRepository.save(tc);

        recalcularCategoria(clienteId);
        notificacionService.notificarMedioPagoAgregado(cliente, mp.getDescripcion());

        return toResumen(mp);
    }

    @Transactional
    public MedioPagoResumen agregarCheque(String email, ChequeCertificadoRequest req, MultipartFile foto) {
        Integer clienteId = getClienteId(email);
        // Verificar unicidad del cheque solo dentro de los medios de pago del cliente
        boolean chequeYaRegistrado = medioPagoRepository
                .findByClienteIdentificadorAndEliminadoFalse(clienteId).stream()
                .anyMatch(mp -> chequeCertificadoRepository.findByMedioPagoId(mp.getId())
                        .map(ch -> ch.getNumeroCheque().equals(req.getNumeroCheque()))
                        .orElse(false));
        if (chequeYaRegistrado) {
            throw new ConflictException("Ya tenés un cheque registrado con ese número");
        }
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        MedioPago mp = new MedioPago();
        mp.setCliente(cliente);
        mp.setTipo("cheque_certificado");
        mp.setDescripcion("Cheque certificado " + req.getBancoEmisor().trim() + " - Nro. " + req.getNumeroCheque().trim());
        mp = medioPagoRepository.save(mp);

        ChequeCertificado ch = new ChequeCertificado();
        ch.setMedioPagoId(mp.getId());
        ch.setBancoEmisor(req.getBancoEmisor());
        ch.setMontoCertificado(req.getMontoCertificado());
        ch.setNumeroCheque(req.getNumeroCheque());
        try {
            if (foto != null && !foto.isEmpty())
                ch.setFotoCheque(foto.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("Error al procesar la foto del cheque");
        }
        chequeCertificadoRepository.save(ch);

        recalcularCategoria(clienteId);
        notificacionService.notificarMedioPagoAgregado(cliente, mp.getDescripcion());

        return toResumen(mp);
    }

    @Transactional
    public void eliminar(String email, Integer medioPagoId) {
        Integer clienteId = getClienteId(email);
        MedioPago mp = medioPagoRepository
                .findByIdAndClienteIdentificadorAndEliminadoFalse(medioPagoId, clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Medio de pago no encontrado"));
        String descripcion = mp.getDescripcion();
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        mp.setEliminado(true);
        medioPagoRepository.save(mp);
        recalcularCategoria(clienteId);
        notificacionService.notificarMedioPagoEliminado(cliente, descripcion);
    }

    public void recalcularCategoria(Integer clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        List<String> tipos = medioPagoRepository.findByClienteIdentificadorAndEliminadoFalse(clienteId)
                .stream().map(MedioPago::getTipo).distinct().collect(Collectors.toList());

        boolean tieneCuenta = tipos.contains("cuenta_bancaria");
        boolean tieneTarjeta = tipos.contains("tarjeta_credito");
        boolean tieneCheque = tipos.contains("cheque_certificado");
        boolean tieneLosTres = tieneCuenta && tieneTarjeta && tieneCheque;

        long subastasGanadas = pujoRepository.findGanadoresByClienteId(clienteId).stream()
                .map(p -> p.getItem().getCatalogo().getSubasta().getIdentificador())
                .distinct().count();

        String nuevaCategoria;
        if (tieneLosTres && subastasGanadas >= 3) {
            nuevaCategoria = "platino";
        } else if (tieneLosTres && subastasGanadas >= 1) {
            nuevaCategoria = "oro";
        } else if (tieneLosTres) {
            nuevaCategoria = "plata";
        } else if (tipos.size() >= 2) {
            nuevaCategoria = "especial";
        } else {
            nuevaCategoria = "comun";
        }

        cliente.setCategoria(nuevaCategoria);
        clienteRepository.save(cliente);
    }


    public MedioPago validarMedioPagoDelCliente(Integer medioPagoId, Integer clienteId) {
        return medioPagoRepository
                .findByIdAndClienteIdentificadorAndEliminadoFalse(medioPagoId, clienteId)
                .orElseThrow(() -> new ForbiddenException("El medio de pago no pertenece al usuario"));
    }

    private MedioPagoResumen toResumen(MedioPago mp) {
        MedioPagoResumen r = new MedioPagoResumen();
        r.setId(mp.getId());
        r.setTipo(mp.getTipo());
        r.setDescripcion(mp.getDescripcion());
        r.setVerificado(mp.isVerificado());
        if ("cheque_certificado".equals(mp.getTipo())) {
            chequeCertificadoRepository.findByMedioPagoId(mp.getId())
                    .ifPresent(ch -> r.setMontoDisponible(ch.getMontoCertificado()));
        }
        if ("cuenta_bancaria".equals(mp.getTipo())) {
            cuentaBancariaRepository.findByMedioPagoId(mp.getId())
                    .ifPresent(cb -> r.setMontoDisponible(cb.getFondosReservados()));
        }
        return r;
    }

    private Integer getClienteId(String email) {
        return credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getPersonaId();
    }
}
