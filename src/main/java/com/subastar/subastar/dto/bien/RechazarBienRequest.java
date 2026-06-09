package com.subastar.subastar.dto.bien;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RechazarBienRequest {

    @NotBlank(message = "El motivo de rechazo es obligatorio")
    private String motivo;
}
