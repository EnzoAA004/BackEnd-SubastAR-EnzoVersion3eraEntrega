package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BienResumen {
    private Integer id;
    private String nombre;
    private String estado;
    @JsonProperty("subasta_asignada")
    private String subastaAsignada;
    @JsonProperty("precio_base")
    private BigDecimal precioBase;
    private BigDecimal comision;
    @JsonProperty("motivo_rechazo")
    private String motivoRechazo;
    @JsonProperty("costo_envio")
    private BigDecimal costoEnvio;
    @JsonProperty("acepto_condiciones")
    private Boolean aceptoCondiciones;
    @JsonProperty("ubicacion_deposito")
    private String ubicacionDeposito;
    @JsonProperty("poliza_id")
    private String polizaId;
}
