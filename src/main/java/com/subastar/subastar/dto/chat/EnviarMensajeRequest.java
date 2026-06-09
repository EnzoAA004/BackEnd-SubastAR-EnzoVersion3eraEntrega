package com.subastar.subastar.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EnviarMensajeRequest {
    @NotBlank
    @Size(max = 1000)
    private String contenido;
}
