package com.subastar.subastar.dto.medioPago;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CuentaBancariaRequest {
    @NotBlank
    @JsonProperty("nombre_banco")
    private String nombreBanco;

    @NotBlank
    @JsonProperty("pais_banco")
    private String paisBanco;

    @NotBlank
    @JsonProperty("cbu_iban")
    private String cbuIban;

    @NotNull @Positive
    @JsonProperty("fondos_reservados")
    private BigDecimal fondosReservados;
}
