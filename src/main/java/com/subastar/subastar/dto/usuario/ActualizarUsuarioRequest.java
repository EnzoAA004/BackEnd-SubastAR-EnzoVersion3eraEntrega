package com.subastar.subastar.dto.usuario;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ActualizarUsuarioRequest {
    private String domicilio;
    @JsonProperty("pais_origen")
    private String paisOrigen;
}
