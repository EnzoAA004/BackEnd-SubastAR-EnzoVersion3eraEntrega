package com.subastar.subastar.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class ChatNotificacionesResumen {
    @JsonProperty("total_no_leidas")
    private int totalNoLeidas;

    @JsonProperty("hay_no_leidas")
    private boolean hayNoLeidas;

    @JsonProperty("por_tipo")
    private Map<String, Integer> porTipo;
}
