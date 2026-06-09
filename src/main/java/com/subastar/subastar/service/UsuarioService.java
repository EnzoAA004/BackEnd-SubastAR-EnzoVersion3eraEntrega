package com.subastar.subastar.service;

import com.subastar.subastar.dto.usuario.*;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService {

    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;
    private final PaisRepository paisRepository;
    private final PersonaRepository personaRepository;
    private final PujoRepository pujoRepository;
    private final RegistroDeSubastaRepository registroDeSubastaRepository;
    private final MultaRepository multaRepository;
    private final PujoExtraRepository pujoExtraRepository;

    public UsuarioDetalle getMe(String email) {
        Credencial cred = credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Cliente cliente = clienteRepository.findById(cred.getPersonaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        return buildDetalle(cliente, email);
    }

    @Transactional
    public UsuarioDetalle actualizarMe(String email, ActualizarUsuarioRequest req) {
        Credencial cred = credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Cliente cliente = clienteRepository.findById(cred.getPersonaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));
        Persona persona = cliente.getPersona();

        if (req.getDomicilio() != null) {
            persona.setDireccion(req.getDomicilio());
            personaRepository.save(persona);
        }
        if (req.getPaisOrigen() != null) {
            Pais pais = paisRepository.findAll().stream()
                    .filter(p -> p.getNombre() != null && p.getNombre().equalsIgnoreCase(req.getPaisOrigen())
                            || p.getNombreCorto() != null && p.getNombreCorto().equalsIgnoreCase(req.getPaisOrigen()))
                    .findFirst().orElse(null);
            if (pais != null) cliente.setPais(pais);
        }
        clienteRepository.save(cliente);
        return buildDetalle(cliente, email);
    }

    public EstadoCuentaResponse getEstadoCuenta(String email) {
        Credencial cred = credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Cliente cliente = clienteRepository.findById(cred.getPersonaId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        BigDecimal multaPendiente = multaRepository.sumMultasPendientesByClienteId(cliente.getIdentificador());
        String estado;
        String mensaje = null;

        if (!"si".equals(cliente.getAdmitido())) {
            estado = "bloqueado";
            mensaje = "Cuenta bloqueada. Contacte al soporte.";
        } else if (multaPendiente != null && multaPendiente.compareTo(BigDecimal.ZERO) > 0) {
            estado = "multado";
            mensaje = "Tenés una multa pendiente de pago de $" + multaPendiente;
        } else {
            estado = "activo";
            mensaje = "Tu cuenta está activa y al día.";
        }
        return new EstadoCuentaResponse(estado, multaPendiente, mensaje);
    }

    public MetricasResponse getMetricas(String email) {
        Credencial cred = credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Integer clienteId = cred.getPersonaId();

        List<Pujo> todosPujos = pujoRepository.findByClienteId(clienteId);
        List<Pujo> ganados = pujoRepository.findGanadoresByClienteId(clienteId);
        List<RegistroDeSubasta> compras = registroDeSubastaRepository.findByClienteIdentificador(clienteId);

        long subastasParticipadas = todosPujos.stream()
                .map(p -> p.getItem().getCatalogo().getSubasta().getIdentificador())
                .distinct().count();
        long subastasGanadas = ganados.stream()
                .map(p -> p.getItem().getCatalogo().getSubasta().getIdentificador())
                .distinct().count();

        double tasaExito = subastasParticipadas > 0
                ? (double) subastasGanadas / subastasParticipadas : 0;

        BigDecimal totalOfertado = todosPujos.stream()
                .map(Pujo::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPagado = compras.stream()
                .map(RegistroDeSubasta::getImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ofertaPromedio = todosPujos.isEmpty() ? BigDecimal.ZERO
                : totalOfertado.divide(BigDecimal.valueOf(todosPujos.size()), 2, RoundingMode.HALF_UP);
        BigDecimal ofertaMasAlta = todosPujos.stream()
                .map(Pujo::getImporte).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal ofertaMasBaja = todosPujos.stream()
                .map(Pujo::getImporte).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

        // Ganadas por mes
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Long> porMes = ganados.stream()
                .flatMap(p -> {
                    return pujoExtraRepository.findByPujoId(p.getIdentificador())
                            .map(pe -> Map.entry(pe.getTimestampPuja().format(fmt), 1L))
                            .stream();
                })
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.counting()));

        List<MetricasResponse.GanadasPorMes> ganadasPorMes = porMes.entrySet().stream()
                .map(e -> {
                    MetricasResponse.GanadasPorMes g = new MetricasResponse.GanadasPorMes();
                    g.setMes(e.getKey());
                    g.setCantidad(e.getValue().intValue());
                    return g;
                }).collect(Collectors.toList());

        MetricasResponse resp = new MetricasResponse();
        resp.setSubastasParticipadas((int) subastasParticipadas);
        resp.setSubastasGanadas((int) subastasGanadas);
        resp.setTasaExito(tasaExito);
        resp.setTotalOfertado(totalOfertado);
        resp.setTotalPagado(totalPagado);
        resp.setOfertaPromedio(ofertaPromedio);
        resp.setOfertaMasAlta(ofertaMasAlta);
        resp.setOfertaMasBaja(ofertaMasBaja);
        resp.setGanadasPorMes(ganadasPorMes);
        return resp;
    }

    private UsuarioDetalle buildDetalle(Cliente cliente, String email) {
        UsuarioDetalle d = new UsuarioDetalle();
        d.setId(cliente.getIdentificador());
        Persona p = cliente.getPersona();
        if (p != null) {
            String[] parts = p.getNombre() != null ? p.getNombre().split(" ", 2) : new String[]{"", ""};
            d.setNombre(parts[0]);
            d.setApellido(parts.length > 1 ? parts[1] : "");
            d.setDomicilio(p.getDireccion());
            d.setDni(p.getDocumento());
        }
        d.setEmail(email);
        d.setCategoria(cliente.getCategoria() != null ? cliente.getCategoria() : "comun");
        d.setEstado("si".equals(cliente.getAdmitido()) ? "activo" : "bloqueado");
        if (cliente.getPais() != null) d.setPaisOrigen(cliente.getPais().getNombre());
        return d;
    }
}
