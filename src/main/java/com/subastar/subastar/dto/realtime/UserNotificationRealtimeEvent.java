package com.subastar.subastar.dto.realtime;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserNotificationRealtimeEvent {
    private RealtimeEventType type;
    private Integer notificationId;
    private String tipo;
    private String titulo;
    private String contenido;
    private LocalDateTime timestamp;
    private boolean leido;
    private String url;
    private String target;
}
