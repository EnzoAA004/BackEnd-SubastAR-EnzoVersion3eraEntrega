package com.subastar.subastar.controller;

import com.subastar.subastar.dto.compra.CompraDetalle;
import com.subastar.subastar.dto.compra.CompraResumen;
import com.subastar.subastar.dto.compra.RegularizarPagoRequest;
import com.subastar.subastar.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @GetMapping
    public ResponseEntity<List<CompraResumen>> listar(
            @RequestParam(required = false) String estado_pago,
            @RequestParam(required = false) String estado_entrega,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(compraService.listar(user.getUsername(), estado_pago, estado_entrega));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraDetalle> getDetalle(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(compraService.getDetalle(user.getUsername(), id));
    }

    @PostMapping("/{id}/regularizar-pago")
    public ResponseEntity<CompraDetalle> regularizarPago(
            @PathVariable Integer id,
            @Valid @RequestBody RegularizarPagoRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(compraService.regularizarPago(user.getUsername(), id, req));
    }

    @GetMapping("/{id}/factura")
    public ResponseEntity<Map<String, String>> getFactura(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {
        compraService.getDetalle(user.getUsername(), id);
        return ResponseEntity.ok(Map.of(
                "message", "Factura disponible",
                "url", "/api/v1/compras/" + id + "/factura/download"
        ));
    }

    @GetMapping("/{id}/factura/download")
    public ResponseEntity<byte[]> downloadFactura(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {
        CompraDetalle detalle = compraService.getDetalle(user.getUsername(), id);
        String fechaEmision = fecha(LocalDateTime.now());
        String contenido = "FACTURA - SUBASTAR\n"
                + "==================\n\n"
                + "Datos de la compra\n"
                + "------------------\n"
                + "Fecha de emisión: " + fechaEmision + "\n"
                + "Compra ID: " + valor(detalle.getId()) + "\n"
                + "Fecha de compra: " + fecha(detalle.getFecha()) + "\n"
                + "Item: " + valor(detalle.getNombreItem()) + "\n"
                + "Subasta: " + valor(detalle.getSubasta()) + "\n\n"
                + "Importes\n"
                + "--------\n"
                + "Valor pujado: " + dinero(detalle.getValorPujado()) + "\n"
                + "Costo de envío: " + dinero(detalle.getCostoEnvio()) + "\n"
                + "Multa: " + dinero(detalle.getMulta()) + "\n"
                + "Total: " + dinero(detalle.getTotal()) + "\n\n"
                + "Pago\n"
                + "----\n"
                + "Estado de pago: " + valor(detalle.getEstadoPago()) + "\n"
                + "Medio de pago: " + valor(detalle.getMedioPago()) + "\n\n"
                + "Entrega\n"
                + "-------\n"
                + "Estado de entrega: " + valor(detalle.getEstadoEntrega()) + "\n"
                + "Dirección de entrega: " + valor(detalle.getDireccionEntrega()) + "\n\n"
                + "Seguro / póliza\n"
                + "---------------\n"
                + "Póliza ID: " + valor(detalle.getPolizaId()) + "\n"
                + "Número de póliza: " + valor(detalle.getNumeroPoliza()) + "\n\n"
                + "Gracias por operar con SubastAR.\n";
        byte[] bytes = contenido.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factura-" + id + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    private String valor(Object value) {
        return value != null ? value.toString() : "No informado";
    }

    private String dinero(BigDecimal value) {
        return value != null ? "ARS " + value.toPlainString() : "ARS 0.00";
    }

    private String fecha(LocalDateTime value) {
        return value != null ? value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "No informado";
    }
}
