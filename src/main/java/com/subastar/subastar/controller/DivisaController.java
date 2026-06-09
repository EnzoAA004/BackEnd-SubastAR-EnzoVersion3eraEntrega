package com.subastar.subastar.controller;

import com.subastar.subastar.dto.divisa.DivisaResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/divisas")
public class DivisaController {

    @GetMapping
    public ResponseEntity<List<DivisaResponse>> listarDivisas() {
        return ResponseEntity.ok(List.of(
                new DivisaResponse("ARS", "Peso argentino", "$"),
                new DivisaResponse("USD", "Dolar estadounidense", "US$")
        ));
    }
}
