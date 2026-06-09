package com.subastar.subastar.dto.compra;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegularizarPagoRequest {
    @NotNull
    @JsonProperty("medio_pago_id")
    private Integer medioPagoId;
}
