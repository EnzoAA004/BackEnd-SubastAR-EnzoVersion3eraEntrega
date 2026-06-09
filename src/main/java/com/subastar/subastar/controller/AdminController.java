package com.subastar.subastar.controller;

import com.subastar.subastar.dto.bien.RechazarBienRequest;
import com.subastar.subastar.service.AuthService;
import com.subastar.subastar.service.BienService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final BienService bienService;

    @PostMapping("/registros/{id}/aprobar")
    public ResponseEntity<Map<String, String>> aprobarRegistro(@PathVariable Integer id) {
        authService.aprobarRegistro(id);
        return ResponseEntity.ok(Map.of("message", "Registro aprobado. Se envió el código al usuario por email."));
    }

    @PostMapping("/bienes/{id}/rechazar")
    public ResponseEntity<Map<String, String>> rechazarBien(
            @PathVariable Integer id,
            @Valid @RequestBody RechazarBienRequest req) {
        bienService.rechazarBien(id, req.getMotivo());
        return ResponseEntity.ok(Map.of("message", "Bien rechazado y notificación enviada al usuario."));
    }
}
