package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BienFotoResponse {
    @JsonProperty("codigo_foto")
    private String codigoFoto;

    @JsonProperty("nombre_archivo")
    private String nombreArchivo;

    @JsonProperty("public_id")
    private String publicId;

    private String url;

    private String tipo;
}
