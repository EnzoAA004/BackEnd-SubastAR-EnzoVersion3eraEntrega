package com.subastar.subastar.service;

import com.subastar.subastar.dto.seguro.AumentarPolizaRequest;
import com.subastar.subastar.dto.seguro.PolizaResponse;
import com.subastar.subastar.exception.ForbiddenException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SeguroService {

    private final SeguroRepository seguroRepository;
    private final SeguroExtraRepository seguroExtraRepository;
    private final ProductoRepository productoRepository;
    private final ProductoDetalleRepository productoDetalleRepository;
    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;

    public List<PolizaResponse> listarMisPolizas(String email) {
        Integer clienteId = getClienteId(email);

        // Pólizas como comprador (beneficiario en seguros_extra)
        java.util.Set<String> polizasIds = new java.util.LinkedHashSet<>();
        java.util.Map<String, SeguroExtra> extrasMap = new java.util.LinkedHashMap<>();
        seguroExtraRepository.findByBeneficiarioId(clienteId)
                .forEach(e -> { polizasIds.add(e.getPolizaId()); extrasMap.put(e.getPolizaId(), e); });

        // Pólizas como dueño de un bien publicado (productos_detalle.poliza_id)
        productoDetalleRepository.findByClienteIdAndPolizaIdIsNotNull(clienteId)
                .forEach(det -> polizasIds.add(det.getPolizaId()));

        return polizasIds.stream()
                .map(polizaId -> seguroRepository.findById(polizaId)
                        .map(seguro -> toPolizaResponse(seguro, extrasMap.get(polizaId), clienteId))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    public PolizaResponse getPoliza(String email, String polizaId) {
        // Valida que el usuario autenticado exista en el sistema.
        getClienteId(email);
        Seguro seguro = seguroRepository.findById(polizaId)
                .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada"));
        SeguroExtra extra = seguroExtraRepository.findById(polizaId).orElse(null);

        return toPolizaResponseFrontend(seguro, extra);
    }

    @Transactional
    public PolizaResponse ampliarPoliza(String email, String polizaId, AumentarPolizaRequest req) {
        Integer clienteId = getClienteId(email);
        Seguro seguro = seguroRepository.findById(polizaId)
                .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada"));
        SeguroExtra extra = seguroExtraRepository.findById(polizaId).orElse(null);

        boolean esDuenioDePoliza = productoDetalleRepository.existsByClienteIdAndPolizaId(clienteId, polizaId);

        // Autoasigna beneficiario cuando aún no fue definido, solo si el usuario es dueño de un bien con esta póliza.
        if (extra == null) {
            if (!esDuenioDePoliza) {
                throw new ForbiddenException("La póliza no pertenece al usuario");
            }
            extra = new SeguroExtra();
            extra.setPolizaId(polizaId);
            extra.setBeneficiarioId(clienteId);
            extra = seguroExtraRepository.save(extra);
        } else if (extra.getBeneficiarioId() == null) {
            if (!esDuenioDePoliza) {
                throw new ForbiddenException("La póliza no pertenece al usuario");
            }
            extra.setBeneficiarioId(clienteId);
            extra = seguroExtraRepository.save(extra);
        } else if (!extra.getBeneficiarioId().equals(clienteId)) {
            throw new ForbiddenException("La póliza no pertenece al usuario");
        }

        if (req.getNuevoValorAsegurado().compareTo(seguro.getImporte()) <= 0) {
            throw new com.subastar.subastar.exception.BadRequestException(
                    "El nuevo valor asegurado debe ser mayor al actual (" + seguro.getImporte() + ")");
        }

        seguro.setImporte(req.getNuevoValorAsegurado());
        seguroRepository.save(seguro);
        return toPolizaResponse(seguro, extra, clienteId);
    }

    private PolizaResponse toPolizaResponse(Seguro seguro, SeguroExtra extra, Integer clienteId) {
        PolizaResponse r = new PolizaResponse();
        r.setNumeroPoliza(seguro.getNroPoliza());
        r.setAseguradora(seguro.getCompania());
        r.setValorAsegurado(seguro.getImporte());

        if (extra != null) {
            r.setVigenciaDesde(extra.getVigenciaDesde());
            r.setVigenciaHasta(extra.getVigenciaHasta());
            r.setCobertura(extra.getCobertura());

            if (extra.getBeneficiarioId() != null) {
                clienteRepository.findById(extra.getBeneficiarioId()).ifPresent(c -> {
                    Persona p = c.getPersona();
                    if (p != null) r.setBeneficiario(p.getNombre());
                });
            }

            PolizaResponse.ContactoAseguradora contacto = new PolizaResponse.ContactoAseguradora();
            contacto.setTelefono(extra.getContactoTelefono());
            contacto.setEmail(extra.getContactoEmail());
            contacto.setWeb(extra.getContactoWeb());
            r.setContactoAseguradora(contacto);
        }

        List<String> piezas = productoRepository.findAll().stream()
                .filter(p -> seguro.getNroPoliza().equals(p.getSeguroNroPoliza()))
                .map(p -> p.getDescripcionCatalogo() != null ? p.getDescripcionCatalogo() : "Producto #" + p.getIdentificador())
                .collect(java.util.stream.Collectors.toList());
        r.setPiezas(piezas);

        return r;
    }

    private PolizaResponse toPolizaResponseFrontend(Seguro seguro, SeguroExtra extra) {
        PolizaResponse r = new PolizaResponse();
        r.setNumeroPoliza(seguro.getNroPoliza());
        r.setAseguradora(seguro.getCompania());
        r.setValorAsegurado(seguro.getImporte());

        if (extra != null) {
            r.setVigenciaDesde(extra.getVigenciaDesde());
            r.setVigenciaHasta(extra.getVigenciaHasta());
            r.setCobertura(extra.getCobertura());

            String telefono = extra.getContactoTelefono();
            String email = extra.getContactoEmail();
            if ((telefono != null && !telefono.isBlank()) || (email != null && !email.isBlank())) {
                PolizaResponse.ContactoAseguradora contacto = new PolizaResponse.ContactoAseguradora();
                contacto.setTelefono(telefono);
                contacto.setEmail(email);
                contacto.setWeb(null);
                r.setContactoAseguradora(contacto);
            } else {
                r.setContactoAseguradora(null);
            }
        } else {
            r.setVigenciaDesde(null);
            r.setVigenciaHasta(null);
            r.setCobertura(null);
            r.setContactoAseguradora(null);
        }

        List<String> piezas = productoRepository.findAll().stream()
                .filter(p -> seguro.getNroPoliza().equals(p.getSeguroNroPoliza()))
                .map(p -> p.getDescripcionCatalogo() != null ? p.getDescripcionCatalogo() : "Producto #" + p.getIdentificador())
                .collect(Collectors.toList());
        r.setPiezas(piezas.isEmpty() ? Collections.emptyList() : piezas);

        aplicarFallbackDemo(r, seguro);

        return r;
    }

    private void aplicarFallbackDemo(PolizaResponse r, Seguro seguro) {
        boolean esDemo = "POL-LIVE-001".equalsIgnoreCase(seguro.getNroPoliza())
                || "Aseguradora Demo".equalsIgnoreCase(seguro.getCompania());
        if (!esDemo) {
            return;
        }

        if (r.getCobertura() == null || r.getCobertura().isBlank()) {
            r.setCobertura("Cobertura total contra robo, dano y perdida accidental. Cubre el 50% del valor declarado.");
        }

        if (r.getPiezas() == null || r.getPiezas().isEmpty()) {
            r.setPiezas(List.of("Objeto principal", "Accesorios incluidos"));
        }

        if (r.getContactoAseguradora() == null) {
            PolizaResponse.ContactoAseguradora contacto = new PolizaResponse.ContactoAseguradora();
            contacto.setTelefono("+54 11 4000-1234");
            contacto.setEmail("siniestros@aseguradorademo.com");
            contacto.setWeb(null);
            r.setContactoAseguradora(contacto);
        }
    }

    private Integer getClienteId(String email) {
        return credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getPersonaId();
    }
}
