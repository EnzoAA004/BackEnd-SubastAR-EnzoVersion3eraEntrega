package com.subastar.subastar.dto.realtime;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AuctionRealtimeEvent {
    private RealtimeEventType type;
    private Integer subastaId;
    private Integer itemId;
    private Integer pujaId;
    private BigDecimal monto;
    private String nombreUsuario;
    private LocalDateTime timestamp;
}
