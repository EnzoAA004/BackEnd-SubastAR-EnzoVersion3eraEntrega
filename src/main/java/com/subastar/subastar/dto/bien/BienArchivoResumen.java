package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BienArchivoResumen {
    @JsonProperty("codigo_archivo")
    private String codigoArchivo;
    @JsonProperty("nombre_archivo")
    private String nombreArchivo;
    @JsonProperty("tipo_archivo")
    private String tipoArchivo;
    @JsonProperty("url_temporal")
    private String urlTemporal;
}
