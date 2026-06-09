package com.subastar.subastar.dto.bien;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CrearBienSolicitudRequest {
    @NotBlank
    private String tipo;
}
