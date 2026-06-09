package com.subastar.subastar.dto.subasta;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemCatalogoResponse {
    private Integer id;
    @JsonProperty("numero_pieza")
    private String numeroPieza;
    private String nombre;
    private String descripcion;
    @JsonProperty("precio_base")
    private BigDecimal precioBase;
    private String estado;
    private List<String> imagenes;
    @JsonProperty("dueno_actual")
    private String duenoActual;
    private String artista;
    @JsonProperty("fecha_creacion")
    private String fechaCreacion;
    private String historia;
}
