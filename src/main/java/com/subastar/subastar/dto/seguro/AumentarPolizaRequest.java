package com.subastar.subastar.dto.seguro;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class AumentarPolizaRequest {
    @NotNull @Positive
    @JsonProperty("nuevo_valor_asegurado")
    private BigDecimal nuevoValorAsegurado;
}
