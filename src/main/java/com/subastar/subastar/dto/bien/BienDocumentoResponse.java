package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BienDocumentoResponse {
    @JsonProperty("codigo_documento")
    private String codigoDocumento;

    @JsonProperty("nombre_archivo")
    private String nombreArchivo;

    private String url;

    private String tipo;

    @JsonProperty("content_type")
    private String contentType;
}
