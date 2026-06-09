package com.subastar.subastar.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MensajeChatResponse {
    private Integer id;
    private String emisor;
    private String contenido;
    private LocalDateTime timestamp;
    private boolean leido;

    @JsonProperty("mensaje")
    public String getMensaje() {
        return contenido;
    }

    @JsonProperty("mensaje")
    public void setMensaje(String mensaje) {
        this.contenido = mensaje;
    }
}
