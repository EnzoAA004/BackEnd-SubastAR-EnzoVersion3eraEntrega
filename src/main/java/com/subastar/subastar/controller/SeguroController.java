package com.subastar.subastar.controller;

import com.subastar.subastar.dto.seguro.AumentarPolizaRequest;
import com.subastar.subastar.dto.seguro.PolizaResponse;
import com.subastar.subastar.service.SeguroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seguros")
@RequiredArgsConstructor
public class SeguroController {

    private final SeguroService seguroService;

    @GetMapping
    public ResponseEntity<List<PolizaResponse>> listarMisPolizas(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(seguroService.listarMisPolizas(user.getUsername()));
    }

    @GetMapping("/{polizaId}")
    public ResponseEntity<PolizaResponse> getPoliza(
            @PathVariable String polizaId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(seguroService.getPoliza(user.getUsername(), polizaId));
    }

    @PostMapping("/{polizaId}/ampliar")
    public ResponseEntity<PolizaResponse> ampliarPoliza(
            @PathVariable String polizaId,
            @Valid @RequestBody AumentarPolizaRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(seguroService.ampliarPoliza(user.getUsername(), polizaId, req));
    }
}
