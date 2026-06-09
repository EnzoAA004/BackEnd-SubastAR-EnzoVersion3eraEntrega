package com.subastar.subastar.dto.puja;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PujaRequest {
    @NotNull @Positive
    private BigDecimal monto;

    @NotNull
    @JsonProperty("medio_pago_id")
    private Integer medioPagoId;
}
