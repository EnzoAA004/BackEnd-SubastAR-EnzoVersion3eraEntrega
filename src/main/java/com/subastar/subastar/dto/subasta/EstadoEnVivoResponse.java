package com.subastar.subastar.dto.subasta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subastar.subastar.dto.puja.PujaResumen;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class EstadoEnVivoResponse {
    @JsonProperty("item_actual")
    private ItemCatalogoResponse itemActual;
    @JsonProperty("mejor_oferta")
    private BigDecimal mejorOferta;
    @JsonProperty("mejor_postor")
    private String mejorPostor;
    @JsonProperty("puja_minima")
    private BigDecimal pujaMinima;
    @JsonProperty("puja_maxima")
    private BigDecimal pujaMaxima;
    @JsonProperty("historial_pujas")
    private List<PujaResumen> historialPujas;
    @JsonProperty("segundos_restantes")
    private Integer segundosRestantes;
}
