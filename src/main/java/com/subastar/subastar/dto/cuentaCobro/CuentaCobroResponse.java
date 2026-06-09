package com.subastar.subastar.dto.cuentaCobro;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CuentaCobroResponse {
    private Integer id;

    @JsonProperty("nombre_banco")
    private String nombreBanco;

    @JsonProperty("cbu_iban")
    private String cbuIban;

    private String pais;

    private String moneda;
}
