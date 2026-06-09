package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActualizarBienRequest {
    @JsonProperty("informacion_adicional")
    @Size(max = 4000)
    private String informacionAdicional;

    @JsonProperty("precio_base_sugerido")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal precioBaseSugerido;

    @JsonProperty("divisa_precio_base_sugerido")
    private String divisaPrecioBaseSugerido;
}
