package com.subastar.subastar.dto.chat;

import lombok.Data;

import java.util.List;

@Data
public class MarcarNotificacionesLeidasRequest {
    private List<String> tipos;
}
