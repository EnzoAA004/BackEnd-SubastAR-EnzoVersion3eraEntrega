package com.subastar.subastar.controller;

import com.subastar.subastar.dto.puja.PujaRequest;
import com.subastar.subastar.dto.puja.PujaResumen;
import com.subastar.subastar.dto.subasta.*;
import com.subastar.subastar.service.PujaService;
import com.subastar.subastar.service.SubastaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subastas")
@RequiredArgsConstructor
public class SubastaController {

    private final SubastaService subastaService;
    private final PujaService pujaService;

    @GetMapping
    public ResponseEntity<List<SubastaResumen>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(subastaService.listar(estado, categoria, moneda, busqueda));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubastaDetalle> getDetalle(@PathVariable Integer id) {
        return ResponseEntity.ok(subastaService.getDetalle(id));
    }

    @GetMapping("/{id}/catalogo")
    public ResponseEntity<List<ItemCatalogoResponse>> getCatalogo(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(subastaService.getCatalogo(id, user != null));
    }

    @GetMapping("/{id}/catalogo/{item_id}")
    public ResponseEntity<ItemCatalogoResponse> getItem(
            @PathVariable Integer id,
            @PathVariable("item_id") Integer itemId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(subastaService.getItem(id, itemId, user != null));
    }

    @GetMapping("/{id}/en-vivo")
    public ResponseEntity<EstadoEnVivoResponse> getEnVivo(@PathVariable Integer id) {
        return ResponseEntity.ok(subastaService.getEnVivo(id));
    }

    @PostMapping("/{id}/pujas")
    public ResponseEntity<PujaResumen> pujar(
            @PathVariable Integer id,
            @Valid @RequestBody PujaRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pujaService.pujar(id, req, user.getUsername()));
    }

    @GetMapping("/{id}/pujas/{item_id}")
    public ResponseEntity<List<PujaResumen>> getHistorialPujas(
            @PathVariable Integer id,
            @PathVariable("item_id") Integer itemId) {
        return ResponseEntity.ok(pujaService.getHistorialPujas(id, itemId));
    }

    @GetMapping("/{id}/resultado/{item_id}")
    public ResponseEntity<ResultadoPujaResponse> getResultado(
            @PathVariable Integer id,
            @PathVariable("item_id") Integer itemId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(subastaService.getResultado(id, itemId, user.getUsername()));
    }
}
