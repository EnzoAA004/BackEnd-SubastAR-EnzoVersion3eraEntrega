package com.subastar.subastar.security;

import com.subastar.subastar.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

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
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            validateSubscribe(accessor);
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            String username = accessor.getUser() != null ? accessor.getUser().getName() : "anonimo";
            log.info("STOMP disconnect user={}", username);
        }

        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("STOMP connect without bearer token");
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

    private void validateSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        String username = accessor.getUser() != null ? accessor.getUser().getName() : "anonimo";

        if ("/user/queue/notificaciones".equals(destination) && accessor.getUser() == null) {
            log.info("STOMP subscribe rejected user={} destination={}", username, destination);
            throw new AccessDeniedException("La cola privada de notificaciones requiere autenticacion");
        }

        log.info("STOMP subscribe user={} destination={}", username, destination);
    }
}
