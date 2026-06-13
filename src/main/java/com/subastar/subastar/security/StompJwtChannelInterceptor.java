package com.subastar.subastar.security;

import com.subastar.subastar.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateIfPresent(accessor, true);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return validateSubscribe(message, accessor);
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String username = accessor.getUser() != null ? accessor.getUser().getName() : "anonimo";
            log.info("STOMP disconnect user={}", username);
        }

        return message;
    }

    private void authenticateIfPresent(StompHeaderAccessor accessor, boolean connectFrame) {
        String authHeader = getAuthorizationHeader(accessor);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (connectFrame) {
                log.info("STOMP connect without bearer token");
            }
            return;
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isTokenValid(token) || tokenBlacklistRepository.existsByToken(token)) {
            log.info("STOMP connect rejected: invalid or blacklisted token");
            throw new BadCredentialsException("Token STOMP invalido");
        }

        String email = jwtUtil.extractEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        accessor.setUser(authentication);
        log.info("STOMP connect authenticated user={}", email);
    }

    private Message<?> validateSubscribe(Message<?> message, StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            authenticateIfPresent(accessor, false);
        }

        String destination = accessor.getDestination();
        String username = accessor.getUser() != null ? accessor.getUser().getName() : "anonimo";

        if (destination != null && destination.startsWith("/user/") && accessor.getUser() == null) {
            log.info("STOMP subscribe rejected user={} destination={}", username, destination);
            return null;
        }

        log.info("STOMP subscribe user={} destination={}", username, destination);
        return message;
    }

    private String getAuthorizationHeader(StompHeaderAccessor accessor) {
        String exactHeader = accessor.getFirstNativeHeader("Authorization");
        if (exactHeader != null) {
            return exactHeader;
        }

        for (String headerName : accessor.toNativeHeaderMap().keySet()) {
            if ("authorization".equalsIgnoreCase(headerName)) {
                List<String> values = accessor.getNativeHeader(headerName);
                return values == null || values.isEmpty() ? null : values.get(0);
            }
        }

        return null;
    }
}
