package dev.irakodes.triviablitz.controller;

import dev.irakodes.triviablitz.dto.*;
import dev.irakodes.triviablitz.event.*;
import dev.irakodes.triviablitz.model.GameRoom;
import dev.irakodes.triviablitz.model.GameStatus;
import dev.irakodes.triviablitz.model.Question;
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

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory
            .getLogger(GameRoomController.class);

    private final static int QUESTION_TIME_LIMIT_SECONDS = 20;

    private final GameService gameService;
    private final CountdownService countdownService;
    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate msgTemplate;

    public GameRoomController(GameService gameService, CountdownService countdownService, SessionRegistry sessionRegistry, SimpMessagingTemplate mgsTemplate) {
        this.gameService = gameService;
        this.countdownService = countdownService;
        this.sessionRegistry = sessionRegistry;
        this.msgTemplate = mgsTemplate;
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

    @MessageMapping("/room/join")
    public void joinRoom(JoinRoomRequest request, SimpMessageHeaderAccessor headers) {
        var sessionId = getSessionId(headers);
        log.info("Player {} joining room {}", request.playerName(), request.roomCode());

        var room = gameService.joinRoom(request.roomCode(), request.playerName(),
                sessionId);
        sessionRegistry.register(sessionId, room.getCode());

        broadcastRoomState(room);
    }

    @MessageMapping("/room/start")
    public void startGame(StartGameRequest request, SimpMessageHeaderAccessor headers) {
        var sessionId = getSessionId(headers);
        var question = gameService.startGame(request.roomCode(), sessionId);
        var room = gameService.getRoom(request.roomCode())
                .orElseThrow();

        var questionIndex = room.getCurrentQuestionIndex();
        broadcastQuestion(room.getCode(), questionIndex, question);

        countdownService.startCountdown(room.getCode(),
                QUESTION_TIME_LIMIT_SECONDS,
                () -> completeQuestion(room.getCode(), questionIndex));

        throw new UnsupportedOperationException("Not implemented yet");
    }

    private void completeQuestion(String roomCode, int expectedQuestionIndex) {
        var optionalRoom = gameService.getRoom(roomCode);
        if (optionalRoom.isEmpty())
            return;

        var room = optionalRoom.get();
        synchronized (room) {
            if (room.getStatus() != GameStatus.IN_PROGRESS || room
                    .getCurrentQuestionIndex() != expectedQuestionIndex)
                return;
        }

        var finalQuestion = expectedQuestionIndex >= gameService.getQuestions()
                .size() - 1;
        msgTemplate.convertAndSend(topic(roomCode),
                toLeaderboardEvent(roomCode, finalQuestion));

        if (finalQuestion) {
            gameService.finishGame(roomCode);
            msgTemplate.convertAndSend(topic(roomCode), toGameOverEvent(roomCode));
            return;
        }

        var nextQuestion = gameService.advanceToNextQuestion(roomCode)
                .orElseThrow();
        var updatedRoom = gameService.getRoom(roomCode)
                .orElseThrow();
        var nextQuestionIndex = updatedRoom.getCurrentQuestionIndex();

        broadcastQuestion(roomCode, nextQuestionIndex, nextQuestion);

        countdownService.startCountdown(roomCode, QUESTION_TIME_LIMIT_SECONDS,
                () -> completeQuestion(roomCode, nextQuestionIndex));
    }

    private GameOverEvent toGameOverEvent(String roomCode) {
        var scores = gameService.getPlayersSortedByScore(roomCode)
                .stream()
                .map(player -> new ScoreEntry(
                        player.getName(),
                        player.getScore()
                )).toList();

        return new GameOverEvent("GAME_OVER", scores);
    }

    private LeaderboardEvent toLeaderboardEvent(String roomCode, boolean finalQuestion) {
        var scores = gameService.getPlayersSortedByScore(roomCode).stream()
                .map(p -> new ScoreEntry(p.getName(),
                        p.getScore()))
                .toList();

        return new LeaderboardEvent(
                "LEADERBOARD",
                scores,
                finalQuestion
        );
    }

    private void broadcastQuestion(String roomCode, int questionIndex, Question question) {
        msgTemplate.convertAndSend(topic(roomCode), new QuestionEvent(
                "QUESTION",
                questionIndex,
                question.question(),
                question.options(),
                QUESTION_TIME_LIMIT_SECONDS
        ));
    }

    private void broadcastRoomState(GameRoom room) {
        msgTemplate.convertAndSend(topic(room.getCode()), toRoomStateEvent(room));
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
