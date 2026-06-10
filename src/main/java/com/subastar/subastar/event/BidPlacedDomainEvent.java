package com.subastar.subastar.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidPlacedDomainEvent(
        Integer subastaId,
        Integer itemId,
        Integer pujaId,
        BigDecimal monto,
        String nombreUsuario,
        LocalDateTime timestamp
) {
}
