package com.subastar.subastar.dto.medioPago;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MedioPagoResumen {
    private Integer id;
    private String tipo;
    private String descripcion;
    private boolean verificado;
    @JsonProperty("monto_disponible")
    private BigDecimal montoDisponible;
}
