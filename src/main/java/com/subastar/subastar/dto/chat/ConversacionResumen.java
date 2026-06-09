package com.subastar.subastar.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ConversacionResumen {
    private String tipo;
    private String titulo;
    private String subtitulo;
    @JsonProperty("mensajes_no_leidos")
    private int mensajesNoLeidos;
    @JsonProperty("porcentaje_completitud")
    private Integer porcentajeCompletitud;
}
