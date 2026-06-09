package com.subastar.subastar.dto.subasta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResultadoPujaResponse {
    private String estado;
    @JsonProperty("item_id")
    private Integer itemId;
    @JsonProperty("nombre_item")
    private String nombreItem;
    @JsonProperty("fue_ganador")
    private boolean fueGanador;
    @JsonProperty("monto_final")
    private BigDecimal montoFinal;
}
