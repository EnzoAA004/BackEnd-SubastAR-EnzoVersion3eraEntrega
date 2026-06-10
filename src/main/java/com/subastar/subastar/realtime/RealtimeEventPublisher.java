package com.subastar.subastar.realtime;

import com.subastar.subastar.dto.realtime.AuctionRealtimeEvent;
import com.subastar.subastar.dto.realtime.UserNotificationRealtimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishAuctionEvent(Integer subastaId, AuctionRealtimeEvent event) {
        String destination = "/topic/subastas/" + subastaId;
        messagingTemplate.convertAndSend(destination, event);
        log.info("Published auction realtime event type={} destination={} pujaId={}",
                event.getType(), destination, event.getPujaId());
    }

    public void publishUserNotification(String username, UserNotificationRealtimeEvent event) {
        messagingTemplate.convertAndSendToUser(username, "/queue/notificaciones", event);
        log.info("Published user notification realtime event user={} notificationId={}",
                username, event.getNotificationId());
    }
}
