package com.subastar.subastar.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    @Value("${cloudinary.cloud_name:}")
    private String cloudName;

    @Value("${cloudinary.api_key:}")
    private String apiKey;

    @Value("${cloudinary.api_secret:}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            log.warn("Cloudinary no está configurado (propiedades ausentes). Las subidas remotas estarán deshabilitadas.");
            return;
        }
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
        log.info("Cloudinary inicializado para cloud_name={}", cloudName);
    }

    public String upload(byte[] data, String filename) {
        if (cloudinary == null) {
            log.debug("Intento de subir archivo pero Cloudinary no está inicializado");
            return null;
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(data, ObjectUtils.asMap("resource_type", "auto", "public_id", filename));
            String url = (String) result.get("secure_url");
            log.info("Archivo subido a Cloudinary: {} -> {}", filename, url);
            return url;
        } catch (Exception e) {
            log.error("Error subiendo archivo a Cloudinary: {}", filename, e);
            return null;
        }
    }
}

