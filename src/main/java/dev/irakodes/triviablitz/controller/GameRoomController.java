package dev.irakodes.triviablitz.controller;

import dev.irakodes.triviablitz.dto.CreateRoomRequest;
import dev.irakodes.triviablitz.dto.JoinRoomRequest;
import dev.irakodes.triviablitz.dto.PlayerView;
import dev.irakodes.triviablitz.dto.RoomStateEvent;
import dev.irakodes.triviablitz.event.AnswerAckEvent;
import dev.irakodes.triviablitz.model.GameRoom;
import dev.irakodes.triviablitz.registry.SessionRegistry;
import dev.irakodes.triviablitz.service.CountdownService;
import dev.irakodes.triviablitz.service.GameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class GameRoomController {

    private final static int QUESTION_TIME_LIMIT_SECONDS = 20;

    private final GameService gameService;
    private final CountdownService countdownService;
    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate mgsTemplate;

    public GameRoomController(GameService gameService, CountdownService countdownService, SessionRegistry sessionRegistry, SimpMessagingTemplate mgsTemplate) {
        this.gameService = gameService;
        this.countdownService = countdownService;
        this.sessionRegistry = sessionRegistry;
        this.mgsTemplate = mgsTemplate;
    }

    @SendToUser("/queue/ack")
    @MessageMapping("/room/create")
    public AnswerAckEvent createRoom(CreateRoomRequest request, SimpMessageHeaderAccessor headers) {
        var sessionId = getSessionId(headers);
        var room = gameService.createRoom(request.playerName(), sessionId);
        sessionRegistry.register(sessionId, room.getCode());

        broadcastRoomState(room);

        return new AnswerAckEvent(
                "ANSWER_ACK", true, 0,
                "Room created: " + room.getCode()
        );
    }

    public void joinRoom(JoinRoomRequest request, SimpMessageHeaderAccessor headers) {
        throw new UnsupportedOperationException();
    }

    private void broadcastRoomState(GameRoom room) {
        mgsTemplate.convertAndSend(topic(room.getCode()), toRoomStateEvent(room));
    }

    private String getSessionId(SimpMessageHeaderAccessor headers) {
        var sessionId = headers.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalStateException("Unable to resolve session ID");
        }
        return sessionId;
    }

    private String topic(String code) {
        return "/topic/room/" + code;
    }

    private RoomStateEvent toRoomStateEvent(GameRoom room) {
        var players = room.getPlayers().stream()
                .map(p -> new PlayerView(p.getId(), p.getName(),
                        p.isHost(), p.getScore())).toList();

        return new RoomStateEvent("ROOM_STATE", room.getCode(), players,
                room.getStatus());
    }
}
