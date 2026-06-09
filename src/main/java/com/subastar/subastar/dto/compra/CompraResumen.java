package com.subastar.subastar.dto.compra;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CompraResumen {
    private Integer id;
    @JsonProperty("nombre_item")
    private String nombreItem;
    private String subasta;
    private LocalDateTime fecha;
    @JsonProperty("valor_pujado")
    private BigDecimal valorPujado;
    private BigDecimal multa;
    @JsonProperty("estado_pago")
    private String estadoPago;
    @JsonProperty("estado_entrega")
    private String estadoEntrega;
    @JsonProperty("poliza_id")
    private String polizaId;
    @JsonProperty("numero_poliza")
    private String numeroPoliza;
}
