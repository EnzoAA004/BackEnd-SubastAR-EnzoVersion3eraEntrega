package com.subastar.subastar.service;

import com.subastar.subastar.dto.cuentaCobro.CuentaCobroRequest;
import com.subastar.subastar.dto.cuentaCobro.CuentaCobroResponse;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.CuentaCobro;
import com.subastar.subastar.repository.CuentaCobroRepository;
import com.subastar.subastar.repository.CredencialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CuentaCobroService {

    private final CuentaCobroRepository cuentaCobroRepository;
    private final CredencialRepository credencialRepository;

    public List<CuentaCobroResponse> listar(String email) {
        Integer clienteId = getClienteId(email);
        return cuentaCobroRepository.findByClienteId(clienteId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public CuentaCobroResponse crear(String email, CuentaCobroRequest req) {
        Integer clienteId = getClienteId(email);
        CuentaCobro cc = new CuentaCobro();
        cc.setClienteId(clienteId);
        cc.setNombreBanco(req.getNombreBanco());
        cc.setCbuIban(req.getCbuIban());
        cc.setPais(req.getPais());
        cc.setMoneda(req.getMoneda().toUpperCase());
        return toResponse(cuentaCobroRepository.save(cc));
    }

    private CuentaCobroResponse toResponse(CuentaCobro cc) {
        CuentaCobroResponse r = new CuentaCobroResponse();
        r.setId(cc.getId());
        r.setNombreBanco(cc.getNombreBanco());
        r.setCbuIban(cc.getCbuIban());
        r.setPais(cc.getPais());
        r.setMoneda(cc.getMoneda());
        return r;
    }

    private Integer getClienteId(String email) {
        return credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getPersonaId();
    }
}
