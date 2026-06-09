package com.subastar.subastar.dto.cuentaCobro;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CuentaCobroRequest {
    @NotBlank
    @JsonProperty("nombre_banco")
    private String nombreBanco;

    @NotBlank
    @JsonProperty("cbu_iban")
    private String cbuIban;

    @NotBlank
    private String pais;

    @NotBlank
    @Pattern(regexp = "ARS|USD", message = "La moneda debe ser ARS o USD")
    private String moneda;
}
