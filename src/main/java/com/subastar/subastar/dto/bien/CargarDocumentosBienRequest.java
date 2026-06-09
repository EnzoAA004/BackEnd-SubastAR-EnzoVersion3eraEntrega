package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CargarDocumentosBienRequest {
    @NotNull
    @JsonProperty("declara_propiedad")
    private Boolean declaraPropiedad;
}
