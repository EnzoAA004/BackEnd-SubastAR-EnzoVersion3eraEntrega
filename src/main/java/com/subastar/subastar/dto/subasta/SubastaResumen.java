package com.subastar.subastar.dto.subasta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SubastaResumen {
    private Integer id;
    private String nombre;
    private String direccion;
    @JsonProperty("fecha_inicio")
    private LocalDateTime fechaInicio;
    private String categoria;
    private String moneda;
    private String estado;
    @JsonProperty("total_articulos")
    private Integer totalArticulos;
    private String rematador;
}
