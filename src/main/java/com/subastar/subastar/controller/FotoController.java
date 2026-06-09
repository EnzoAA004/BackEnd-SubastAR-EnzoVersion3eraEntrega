package com.subastar.subastar.controller;

import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.repository.FotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/fotos")
@RequiredArgsConstructor
public class FotoController {

    private final FotoRepository fotoRepository;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getFoto(@PathVariable Integer id) throws IOException {
        byte[] foto = fotoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Foto no encontrada"))
                .getFoto();
        String detectedType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(foto));
        return ResponseEntity.ok()
                .contentType(detectedType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(detectedType))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(foto);
    }
}
