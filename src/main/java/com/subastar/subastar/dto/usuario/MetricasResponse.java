package com.subastar.subastar.dto.usuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class MetricasResponse {
    @JsonProperty("subastas_participadas")
    private int subastasParticipadas;
    @JsonProperty("subastas_ganadas")
    private int subastasGanadas;
    @JsonProperty("tasa_exito")
    private double tasaExito;
    @JsonProperty("total_ofertado")
    private BigDecimal totalOfertado;
    @JsonProperty("total_pagado")
    private BigDecimal totalPagado;
    @JsonProperty("oferta_promedio")
    private BigDecimal ofertaPromedio;
    @JsonProperty("oferta_mas_alta")
    private BigDecimal ofertaMasAlta;
    @JsonProperty("oferta_mas_baja")
    private BigDecimal ofertaMasBaja;
    @JsonProperty("ganadas_por_mes")
    private List<GanadasPorMes> ganadasPorMes;

    @Data
    public static class GanadasPorMes {
        private String mes;
        private int cantidad;
    }
}
