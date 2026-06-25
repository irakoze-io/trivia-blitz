package dev.irakodes.triviablitz.event;

import dev.irakodes.triviablitz.registry.SessionRegistry;
import dev.irakodes.triviablitz.service.CountdownService;
import dev.irakodes.triviablitz.service.GameService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEvenListener {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(WebSocketEvenListener.class);

    private final GameService gaming;
    private final SessionRegistry sessionRegistry;
    private final CountdownService countdownService;

    public WebSocketEvenListener(GameService gs, SessionRegistry sr, CountdownService cs) {
        this.gaming = gs;
        this.sessionRegistry = sr;
        this.countdownService = cs;
    }

    @EventListener(SessionDisconnectEvent.class)
    public void handleSessionDisconnect(SessionDisconnectEvent sde) {
        var accessor = StompHeaderAccessor.wrap(sde.getMessage());
        var sessionId = accessor.getSessionId();

        if (sessionId == null || sessionId.isBlank()) return;
        log.info("Session disconnected: {}", sessionId);

        var roomCode = sessionRegistry.getRoomCode(sessionId);
        var updatedRoom = gaming.removePlayer(sessionId);
        sessionRegistry.unregister(sessionId);

        if (updatedRoom.isPresent()) {
            // broadcast from controller
            return;
        }

        roomCode.ifPresent(countdownService::cancelCountdown);
    }
}
