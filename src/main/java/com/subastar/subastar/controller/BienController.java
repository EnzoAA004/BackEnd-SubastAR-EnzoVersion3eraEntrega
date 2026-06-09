package com.subastar.subastar.controller;

import com.subastar.subastar.dto.bien.*;
import com.subastar.subastar.service.BienService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bienes")
@RequiredArgsConstructor
public class BienController {

    private final BienService bienService;
    private static final Logger log = LoggerFactory.getLogger(BienController.class);

    @PostMapping("/solicitudes")
    public ResponseEntity<BienSolicitudResponse> iniciarSolicitud(
            @Valid @RequestBody CrearBienSolicitudRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Iniciando solicitud de bien para usuario {} tipo={}", user.getUsername(), req.getTipo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bienService.iniciarSolicitud(user.getUsername(), req));
    }

    @PutMapping("/solicitudes/{codigo}/datos")
    public ResponseEntity<BienSolicitudResponse> cargarDatos(
            @PathVariable String codigo,
            @Valid @RequestBody BienDatosRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Cargando datos de solicitud {} por usuario {}", codigo, user.getUsername());
        return ResponseEntity.ok(bienService.cargarDatos(user.getUsername(), codigo, req));
    }

    @PostMapping(value = "/solicitudes/{codigo}/fotos", consumes = "multipart/form-data")
    public ResponseEntity<BienSolicitudResponse> cargarFotos(
            @PathVariable String codigo,
            @RequestPart("fotos") List<MultipartFile> fotos,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Recibiendo petición de cargar {} fotos para solicitud {} por usuario {}", fotos != null ? fotos.size() : 0, codigo, user.getUsername());
        return ResponseEntity.ok(bienService.cargarFotos(user.getUsername(), codigo, fotos));
    }

    @PostMapping(value = "/solicitudes/{codigo}/fotos", consumes = "application/json")
    public ResponseEntity<BienSolicitudResponse> cargarFotosCloudinary(
            @PathVariable String codigo,
            @Valid @RequestBody CloudinaryFotoRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Recibiendo peticion de registrar fotos de Cloudinary para solicitud {} por usuario {}", codigo, user.getUsername());
        return ResponseEntity.ok(bienService.cargarFotosCloudinary(user.getUsername(), codigo, req));
    }

    @DeleteMapping("/solicitudes/{codigo}/fotos/{codigoFoto}")
    public ResponseEntity<Map<String, String>> eliminarFoto(
            @PathVariable String codigo,
            @PathVariable String codigoFoto,
            @AuthenticationPrincipal UserDetails user) {
        bienService.eliminarFoto(user.getUsername(), codigo, codigoFoto);
        return ResponseEntity.ok(Map.of("message", "Foto eliminada correctamente."));
    }

    @PostMapping(value = "/solicitudes/{codigo}/documentos", consumes = "multipart/form-data")
    public ResponseEntity<BienSolicitudResponse> cargarDocumentos(
            @PathVariable String codigo,
            @RequestPart("declara_propiedad") String declaraPropiedad,
            @RequestPart(value = "documentos", required = false) List<MultipartFile> docs,
            @AuthenticationPrincipal UserDetails user) {
        log.info("Recibiendo petición de cargar documentos para solicitud {} por usuario {}. Declara: {}", codigo, user.getUsername(), declaraPropiedad);
        CargarDocumentosBienRequest req = new CargarDocumentosBienRequest();
        req.setDeclaraPropiedad("true".equalsIgnoreCase(declaraPropiedad));
        return ResponseEntity.ok(bienService.cargarDocumentos(user.getUsername(), codigo, req, docs));
    }

    @DeleteMapping("/solicitudes/{codigo}/documentos/{codigoDoc}")
    public ResponseEntity<Map<String, String>> eliminarDocumento(
            @PathVariable String codigo,
            @PathVariable String codigoDoc,
            @AuthenticationPrincipal UserDetails user) {
        bienService.eliminarDocumento(user.getUsername(), codigo, codigoDoc);
        return ResponseEntity.ok(Map.of("message", "Documento eliminado correctamente."));
    }

    @PostMapping("/solicitudes/{codigo}/confirmar")
    public ResponseEntity<BienSolicitudEnviadaResponse> confirmar(
            @PathVariable String codigo,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bienService.confirmar(user.getUsername(), codigo));
    }

    @GetMapping("/mis-bienes")
    public ResponseEntity<List<BienResumen>> getMisBienes(
            @RequestParam(required = false) String estado,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bienService.getMisBienes(user.getUsername(), estado));
    }

    @GetMapping("/mis-bienes/{id}")
    public ResponseEntity<BienDetalle> getMiBien(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bienService.getMiBien(user.getUsername(), id));
    }

    @PatchMapping("/mis-bienes/{id}")
    public ResponseEntity<BienDetalle> actualizarMiBien(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarBienRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bienService.actualizarMiBien(user.getUsername(), id, req));
    }

    @PostMapping("/mis-bienes/{id}/aceptar-condiciones")
    public ResponseEntity<Map<String, String>> aceptarCondiciones(
            @PathVariable Integer id,
            @RequestBody AceptarCondicionesRequest body,
            @AuthenticationPrincipal UserDetails user) {
        bienService.aceptarCondiciones(user.getUsername(), id, body);
        return ResponseEntity.ok(Map.of("message", body.isAcepta() ? "Condiciones aceptadas" : "Condiciones rechazadas"));
    }
}
