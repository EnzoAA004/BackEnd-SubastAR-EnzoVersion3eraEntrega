package com.subastar.subastar.controller;

import com.subastar.subastar.model.Pais;
import com.subastar.subastar.repository.PaisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/paises")
@RequiredArgsConstructor
public class PaisController {

    private final PaisRepository paisRepository;

    @GetMapping
    public ResponseEntity<List<Pais>> listarPaises() {
        return ResponseEntity.ok(paisRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre")));
    }
}

