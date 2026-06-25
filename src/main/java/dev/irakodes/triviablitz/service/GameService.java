package dev.irakodes.triviablitz.service;

import dev.irakodes.triviablitz.event.AnswerAckEvent;
import dev.irakodes.triviablitz.model.GameRoom;
import dev.irakodes.triviablitz.model.GameStatus;
import dev.irakodes.triviablitz.model.Player;
import dev.irakodes.triviablitz.model.Question;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private static final String ROOM_CODE_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int ROOM_CODE_LENGTH = 6;

    private static final int CORRECT_ANSWER_POINTS = 100;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();

    private final QuestionService questionService;

    public GameService(QuestionService questionService) {
        this.questionService = questionService;
    }

    public GameRoom createRoom(String playerName, String sessionId) {
        var normalizedName = validateNamePlayer(playerName);
        var roomCode = generateRoomCode();
        var host = new Player(sessionId, normalizedName, true, 0);

        var room = new GameRoom(roomCode, GameStatus.LOBBY,
                new ArrayList<>(List.of(host)), 0, new HashMap<>());
        rooms.put(roomCode, room);
        return room;
    }

    public GameRoom joinRoom(String code, String playerName, String sessionId) {
        var normalizedCode = normalizeRoomCode(code);
        var normalizedName = validatePlayerName(playerName);
        var room = getRequiredRoom(normalizedCode);
        synchronized (room) {
            if (room.getStatus() != GameStatus.LOBBY) {
                throw new IllegalStateException("Room is no longer accepting players.");
            }
            var alreadyJoined = room.getPlayers().stream()
                    .anyMatch(player -> player.getId().equals(sessionId));
            if (alreadyJoined) {
                throw new IllegalStateException("Player is already in the room.");
            }
            room.getPlayers().add(new Player(sessionId, normalizedName, false, 0));
            return room;
        }
    }

    public Question startGame(String code, String sessionId) {
        var room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            var host = room.getPlayers().stream()
                    .filter(Player::isHost)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Room " + code + " has no host."));

            if (!host.getId().equals(sessionId)) throw new IllegalStateException("Only the host can start the game.");
            if (room.getStatus() != GameStatus.LOBBY) throw new IllegalStateException("Game has already started.");

            room.setStatus(GameStatus.IN_PROGRESS);
            room.setCurrentQuestionIndex(0);
            room.getAnswers().clear();

            resetScores(room);

            return questionService.getAllQuestions().getFirst();
        }
    }

    public AnswerAckEvent submitAnswer(String code, String sessionId, int questionIndex,
                                       String selectedOption) {
        var room = getRequiredRoom(normalizeRoomCode(code));
        var questions = questionService.getAllQuestions();
        synchronized (room) {
            if (room.getStatus() != GameStatus.IN_PROGRESS) {
                throw new IllegalStateException("Game is not in progress.");
            }
            if (room.getCurrentQuestionIndex() != questionIndex) {
                throw new IllegalStateException("Question is no longer active");
            }

            var player = room.getPlayers().stream()
                    .filter(p -> p.getId()
                            .equals(sessionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Player is not part of the room."));

            if (room.getAnswers().containsKey(sessionId)) {
                throw new IllegalStateException("Answer already submitted for this question");
            }

            var normalizedOption = normalizeOption(selectedOption);
            var question = questions.get(questionIndex);

            if (!question.options().containsKey(normalizedOption))
                throw new IllegalArgumentException("Selected option is invalid");

            room.getAnswers().put(sessionId, normalizedOption);
            var correct = question.correctAnswer().equalsIgnoreCase(normalizedOption);

            var points = correct ? CORRECT_ANSWER_POINTS : 0;

            if (correct) player.setScore(player.getScore() + points);

            var message = correct ? "Correct!" : "Incorrect.";

            return new AnswerAckEvent("ANSWER_ACK", correct, points, message);
        }
    }

    public Optional<GameRoom> removePlayer(String sessionId) {
        /*return rooms.values().stream()
                .filter(room -> room.getPlayers().contains(sessionId))
                .findFirst()
                .map(room -> {
                    room.getPlayers().remove(sessionId);
                    return room;
                });*/

        for (var room : rooms.values()) {
            synchronized (room) {
                var removed = room.getPlayers().removeIf(p -> p
                        .getId().equals(sessionId));
                if (!removed) continue;

                room.getAnswers().remove(sessionId);
                if (room.getPlayers().isEmpty()) {
                    rooms.remove(room.getCode());
                    return Optional.empty();
                }

                ensureSingleHost(room);
                return Optional.of(room);
            }
        }

        return Optional.empty();
    }

    public Optional<GameRoom> getRoom(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        return Optional
                .ofNullable(rooms.get(code.trim().toUpperCase(Locale.ROOT)));
    }

    public Optional<Question> advanceToNextQuestion(String code) {
        var room = getRequiredRoom(normalizeRoomCode(code));
        var questions = questionService.getAllQuestions();

        synchronized (room) {
            var nextIndex = room.getCurrentQuestionIndex() + 1;
            room.getAnswers().clear();

            if (nextIndex >= questions.size()) {
                room.setStatus(GameStatus.FINISHED);
                return Optional.empty();
            }

            room.setCurrentQuestionIndex(nextIndex);
            return Optional.of(questions.get(nextIndex));
        }
    }

    public void finishGame(String code) {
        var room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            // room.getStatus();
            room.setStatus(GameStatus.FINISHED);
            room.getAnswers().clear();
        }
    }

    public boolean haveAllPlayersAnswered(String code) {
        var room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            return !room.getPlayers().isEmpty()
                    && room.getAnswers().size() == room.getPlayers().size();
        }
    }

    public List<Player> getPlayersSortedByScore(String code) {
        var room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            return room.getPlayers().stream()
                    .sorted(Comparator
                            .comparingInt(Player::getScore).reversed()
                            .thenComparing(Player::getName))
                    .map(p -> new Player(
                            p.getId(),
                            p.getName(),
                            p.isHost(),
                            p.getScore()
                    ))
                    .toList();
        }
    }

    private void ensureSingleHost(GameRoom room) {
        var hasHost = room.getPlayers().stream()
                .anyMatch(Player::isHost);

        if (!hasHost && !room.getPlayers().isEmpty())
            // room.getPlayers().stream()
            //     .findFirst()
            //     .ifPresent(player -> player.setHost(true));
            room.getPlayers().getFirst().setHost(true);
    }

    private String normalizeOption(String option) {
        if (option == null || option.isBlank())
            throw new IllegalArgumentException("Option cannot be null or blank");
        return option.trim().toUpperCase(Locale.ROOT);
    }

    private void resetScores(GameRoom room) {
        room.getPlayers().forEach(player -> player.setScore(0));
    }

    private GameRoom getRequiredRoom(String code) {
        var room = rooms.get(code);
        if (room == null) throw new IllegalArgumentException("Room not found");

        return room;
    }

    private String normalizeRoomCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Room code is required.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String validatePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name is required.");
        }
        return playerName.trim();
    }

    private String validateNamePlayer(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Player name cannot be null or blank");
        return name.trim();
    }

    private String generateRoomCode() {
        /*var roomCode = new StringBuilder(ROOM_CODE_LENGTH);
        for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
            roomCode.append(ROOM_CODE_ALPHABET.charAt(secureRandom.nextInt(ROOM_CODE_ALPHABET.length())));
        }
        return roomCode.toString();*/
        String code;
        do {
            var builder = new StringBuilder(ROOM_CODE_LENGTH);
            for (var index = 0; index < ROOM_CODE_LENGTH; index++) {
                builder.append(ROOM_CODE_ALPHABET.charAt(secureRandom.nextInt(
                        ROOM_CODE_ALPHABET.length()
                )));
            }
            code = builder.toString();
        } while (rooms.containsKey(code));

        return code;
    }
}
