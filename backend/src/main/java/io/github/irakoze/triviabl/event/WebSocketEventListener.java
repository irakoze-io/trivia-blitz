package io.github.irakoze.triviabl.event;

import java.util.Optional;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import io.github.irakoze.triviabl.controller.GameRoomController;
import io.github.irakoze.triviabl.model.GameRoom;
import io.github.irakoze.triviabl.registry.SessionRegistry;
import io.github.irakoze.triviabl.service.CountdownService;
import io.github.irakoze.triviabl.service.GameService;

@Component
public class WebSocketEventListener {

    private final GameService gameService;
    private final SessionRegistry sessionRegistry;
    private final GameRoomController gameRoomController;
    private final CountdownService countdownService;

    public WebSocketEventListener(GameService gameService,
                                  SessionRegistry sessionRegistry,
                                  GameRoomController gameRoomController,
                                  CountdownService countdownService) {
        this.gameService = gameService;
        this.sessionRegistry = sessionRegistry;
        this.gameRoomController = gameRoomController;
        this.countdownService = countdownService;
    }

    @EventListener(SessionDisconnectEvent.class)
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        Optional<String> roomCode = sessionRegistry.getRoomCode(sessionId);
        Optional<GameRoom> updatedRoom = gameService.removePlayer(sessionId);
        sessionRegistry.unregister(sessionId);

        if (updatedRoom.isPresent()) {
            gameRoomController.broadcastRoomState(updatedRoom.get());
            return;
        }

        roomCode.ifPresent(countdownService::cancelCountdown);
    }
}
