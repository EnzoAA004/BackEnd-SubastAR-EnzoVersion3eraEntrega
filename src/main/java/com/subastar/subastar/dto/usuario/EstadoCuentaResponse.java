package com.subastar.subastar.dto.usuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class EstadoCuentaResponse {
    private String estado;
    @JsonProperty("multa_pendiente")
    private BigDecimal multaPendiente;
    private String mensaje;
}
