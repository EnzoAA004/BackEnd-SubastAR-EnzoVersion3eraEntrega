package com.subastar.subastar.event;

import java.time.LocalDateTime;

public record NotificationCreatedDomainEvent(
        String username,
        Integer notificationId,
        String tipo,
        String titulo,
        String contenido,
        LocalDateTime timestamp,
        boolean leido
) {
}
