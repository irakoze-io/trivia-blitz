package io.github.irakoze.triviabl.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import io.github.irakoze.triviabl.dto.AnswerAckEvent;
import io.github.irakoze.triviabl.dto.CreateRoomRequest;
import io.github.irakoze.triviabl.dto.ErrorEvent;
import io.github.irakoze.triviabl.dto.GameOverEvent;
import io.github.irakoze.triviabl.dto.JoinRoomRequest;
import io.github.irakoze.triviabl.dto.LeaderboardEvent;
import io.github.irakoze.triviabl.dto.PlayerView;
import io.github.irakoze.triviabl.dto.QuestionEvent;
import io.github.irakoze.triviabl.dto.RoomStateEvent;
import io.github.irakoze.triviabl.dto.ScoreEntry;
import io.github.irakoze.triviabl.dto.StartGameRequest;
import io.github.irakoze.triviabl.dto.SubmitAnswerRequest;
import io.github.irakoze.triviabl.model.GameRoom;
import io.github.irakoze.triviabl.model.GameStatus;
import io.github.irakoze.triviabl.model.Question;
import io.github.irakoze.triviabl.registry.SessionRegistry;
import io.github.irakoze.triviabl.service.CountdownService;
import io.github.irakoze.triviabl.service.GameService;

@Controller
public class GameRoomController {

    private static final int QUESTION_TIME_LIMIT_SECONDS = 20;

    private final GameService gameService;
    private final CountdownService countdownService;
    private final SessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    public GameRoomController(GameService gameService,
                              CountdownService countdownService,
                              SessionRegistry sessionRegistry,
                              SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.countdownService = countdownService;
        this.sessionRegistry = sessionRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/room/create")
    @SendToUser("/queue/ack")
    public AnswerAckEvent createRoom(CreateRoomRequest request, SimpMessageHeaderAccessor headers) {
        String sessionId = getSessionId(headers);
        GameRoom room = gameService.createRoom(request.playerName(), sessionId);
        sessionRegistry.register(sessionId, room.getCode());
        broadcastRoomState(room);
        return new AnswerAckEvent("ANSWER_ACK", true, 0, "Room created: " + room.getCode());
    }

    @MessageMapping("/room/join")
    public void joinRoom(JoinRoomRequest request, SimpMessageHeaderAccessor headers) {
        String sessionId = getSessionId(headers);
        GameRoom room = gameService.joinRoom(request.roomCode(), request.playerName(), sessionId);
        sessionRegistry.register(sessionId, room.getCode());
        broadcastRoomState(room);
    }

    @MessageMapping("/room/start")
    public void startGame(StartGameRequest request, SimpMessageHeaderAccessor headers) {
        String sessionId = getSessionId(headers);
        Question question = gameService.startGame(request.roomCode(), sessionId);
        GameRoom room = gameService.getRoom(request.roomCode()).orElseThrow();
        int questionIndex = room.getCurrentQuestionIndex();
        broadcastQuestion(room.getCode(), questionIndex, question);
        countdownService.startCountdown(room.getCode(), QUESTION_TIME_LIMIT_SECONDS,
                () -> completeQuestion(room.getCode(), questionIndex));
    }

    @MessageMapping("/answer/submit")
    @SendToUser("/queue/ack")
    public AnswerAckEvent submitAnswer(SubmitAnswerRequest request, SimpMessageHeaderAccessor headers) {
        String sessionId = getSessionId(headers);
        AnswerAckEvent ackEvent = gameService.submitAnswer(
                request.roomCode(),
                sessionId,
                request.questionIndex(),
                request.selectedOption());

        if (gameService.haveAllPlayersAnswered(request.roomCode())) {
            countdownService.cancelCountdown(request.roomCode());
            completeQuestion(request.roomCode(), request.questionIndex());
        }
        return ackEvent;
    }

    @MessageExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @SendToUser("/queue/ack")
    public ErrorEvent handleGameError(RuntimeException exception) {
        return new ErrorEvent("ERROR", exception.getMessage());
    }

    public void broadcastRoomState(GameRoom room) {
        messagingTemplate.convertAndSend(topic(room.getCode()), toRoomStateEvent(room));
    }

    private void completeQuestion(String roomCode, int expectedQuestionIndex) {
        Optional<GameRoom> optionalRoom = gameService.getRoom(roomCode);
        if (optionalRoom.isEmpty()) {
            return;
        }

        GameRoom room = optionalRoom.get();
        synchronized (room) {
            if (room.getStatus() != GameStatus.IN_PROGRESS || room.getCurrentQuestionIndex() != expectedQuestionIndex) {
                return;
            }
        }

        boolean finalQuestion = expectedQuestionIndex >= gameService.getQuestions().size() - 1;
        messagingTemplate.convertAndSend(topic(roomCode), toLeaderboardEvent(roomCode, finalQuestion));

        if (finalQuestion) {
            gameService.finishGame(roomCode);
            messagingTemplate.convertAndSend(topic(roomCode), toGameOverEvent(roomCode));
            return;
        }

        Question nextQuestion = gameService.advanceToNextQuestion(roomCode).orElseThrow();
        GameRoom updatedRoom = gameService.getRoom(roomCode).orElseThrow();
        int nextQuestionIndex = updatedRoom.getCurrentQuestionIndex();
        broadcastQuestion(roomCode, nextQuestionIndex, nextQuestion);
        countdownService.startCountdown(roomCode, QUESTION_TIME_LIMIT_SECONDS,
                () -> completeQuestion(roomCode, nextQuestionIndex));
    }

    private void broadcastQuestion(String roomCode, int questionIndex, Question question) {
        messagingTemplate.convertAndSend(topic(roomCode), new QuestionEvent(
                "QUESTION",
                questionIndex,
                question.text(),
                question.options(),
                QUESTION_TIME_LIMIT_SECONDS));
    }

    private RoomStateEvent toRoomStateEvent(GameRoom room) {
        List<PlayerView> players = room.getPlayers().stream()
                .map(player -> new PlayerView(player.getId(), player.getName(), player.isHost(), player.getScore()))
                .toList();
        return new RoomStateEvent("ROOM_STATE", room.getCode(), players, room.getStatus());
    }

    private LeaderboardEvent toLeaderboardEvent(String roomCode, boolean isFinal) {
        List<ScoreEntry> scores = gameService.getPlayersSortedByScore(roomCode).stream()
                .map(player -> new ScoreEntry(player.getName(), player.getScore()))
                .toList();
        return new LeaderboardEvent("LEADERBOARD", scores, isFinal);
    }

    private GameOverEvent toGameOverEvent(String roomCode) {
        List<ScoreEntry> scores = gameService.getPlayersSortedByScore(roomCode).stream()
                .map(player -> new ScoreEntry(player.getName(), player.getScore()))
                .toList();
        return new GameOverEvent("GAME_OVER", scores);
    }

    private String topic(String roomCode) {
        return "/topic/room/" + roomCode;
    }

    private String getSessionId(SimpMessageHeaderAccessor headers) {
        String sessionId = headers.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalStateException("Unable to resolve session id.");
        }
        return sessionId;
    }
}
