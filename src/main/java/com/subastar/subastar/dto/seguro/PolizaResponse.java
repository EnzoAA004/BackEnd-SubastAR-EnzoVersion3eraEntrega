package com.subastar.subastar.dto.seguro;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolizaResponse {
    private Integer id;
    @JsonProperty("numero_poliza")
    private String numeroPoliza;
    private String aseguradora;
    private String beneficiario;
    @JsonProperty("valor_asegurado")
    private BigDecimal valorAsegurado;
    @JsonProperty("vigencia_desde")
    private LocalDate vigenciaDesde;
    @JsonProperty("vigencia_hasta")
    private LocalDate vigenciaHasta;
    private String cobertura;
    private List<String> piezas;
    @JsonProperty("contacto_aseguradora")
    private ContactoAseguradora contactoAseguradora;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContactoAseguradora {
        private String telefono;
        private String email;
        private String web;
    }
}
