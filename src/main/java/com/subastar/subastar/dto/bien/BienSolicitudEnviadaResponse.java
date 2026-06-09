package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BienSolicitudEnviadaResponse {
    @JsonProperty("codigo_solicitud")
    private String codigoSolicitud;
    @JsonProperty("codigo_bien")
    private String codigoBien;
    private String estado;
    private String message;
}
