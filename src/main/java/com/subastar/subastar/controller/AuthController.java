package com.subastar.subastar.controller;

import com.subastar.subastar.dto.auth.*;
import com.subastar.subastar.dto.auth.RecuperarPasswordRequest;
import com.subastar.subastar.dto.auth.ConfirmarPasswordRequest;
import com.subastar.subastar.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/registro", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> registro(
            @RequestPart("nombre") String nombre,
            @RequestPart("apellido") String apellido,
            @RequestPart("email") String email,
            @RequestPart("domicilio") String domicilio,
            @RequestPart("pais_origen") String paisOrigen,
            @RequestPart("foto_dni_frente") MultipartFile fotoDniFrente,
            @RequestPart("foto_dni_dorso") MultipartFile fotoDniDorso) {

        RegistroStep1Request req = new RegistroStep1Request();
        req.setNombre(nombre);
        req.setApellido(apellido);
        req.setEmail(email);
        req.setDomicilio(domicilio);
        req.setPaisOrigen(paisOrigen);

        authService.registroStep1(req, fotoDniFrente, fotoDniDorso);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Solicitud recibida. Te notificaremos por email cuando tu cuenta sea aprobada."));
    }

    @PostMapping("/verificar-codigo")
    public ResponseEntity<Map<String, String>> verificarCodigo(@Valid @RequestBody VerificarCodigoRequest req) {
        String token = authService.verificarCodigo(req);
        return ResponseEntity.ok(Map.of(
                "message", "Código verificado correctamente. Podés crear tu contraseña.",
                "tokenVerificacion", token
        ));
    }

    @PostMapping("/completar-registro")
    public ResponseEntity<LoginResponse> completarRegistro(@Valid @RequestBody RegistroStep2Request req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.completarRegistro(req));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente."));
    }

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<Map<String, String>> reenviarCodigo(@Valid @RequestBody ReenviarCodigoRequest req) {
        authService.reenviarCodigo(req);
        return ResponseEntity.ok(Map.of("message", "Se envió un nuevo código al email indicado."));
    }

    @PostMapping("/registro-pendiente/cancelar")
    public ResponseEntity<Map<String, String>> cancelarRegistro(@RequestBody Map<String, String> body) {
        authService.cancelarRegistro(body.get("email"));
        return ResponseEntity.ok(Map.of("message", "Registro cancelado correctamente."));
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<Map<String, String>> recuperarPassword(@Valid @RequestBody RecuperarPasswordRequest req) {
        authService.solicitarRecuperacion(req);
        return ResponseEntity.ok(Map.of("message", "Si el email existe, recibirás un token para restablecer tu contraseña."));
    }

    @PostMapping("/recuperar-password/confirmar")
    public ResponseEntity<Map<String, String>> confirmarPassword(@Valid @RequestBody ConfirmarPasswordRequest req) {
        authService.confirmarRecuperacion(req);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
    }
}
