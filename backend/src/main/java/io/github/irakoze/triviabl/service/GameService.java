package io.github.irakoze.triviabl.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import io.github.irakoze.triviabl.dto.AnswerAckEvent;
import io.github.irakoze.triviabl.model.GameRoom;
import io.github.irakoze.triviabl.model.GameStatus;
import io.github.irakoze.triviabl.model.Player;
import io.github.irakoze.triviabl.model.Question;

@Service
public class GameService {

    private static final String ROOM_CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final int CORRECT_ANSWER_POINTS = 100;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final List<Question> questions = buildQuestions();

    public GameRoom createRoom(String playerName, String sessionId) {
        String normalizedName = validatePlayerName(playerName);
        String roomCode = generateUniqueRoomCode();
        Player host = new Player(sessionId, normalizedName, true, 0);
        GameRoom room = new GameRoom(roomCode, GameStatus.LOBBY, new ArrayList<>(List.of(host)), 0, new HashMap<>());
        rooms.put(roomCode, room);
        return room;
    }

    public GameRoom joinRoom(String code, String playerName, String sessionId) {
        String normalizedCode = normalizeRoomCode(code);
        String normalizedName = validatePlayerName(playerName);
        GameRoom room = getRequiredRoom(normalizedCode);
        synchronized (room) {
            if (room.getStatus() != GameStatus.LOBBY) {
                throw new IllegalStateException("Room is no longer accepting players.");
            }
            boolean alreadyJoined = room.getPlayers().stream().anyMatch(player -> player.getId().equals(sessionId));
            if (alreadyJoined) {
                throw new IllegalStateException("Player is already in the room.");
            }
            room.getPlayers().add(new Player(sessionId, normalizedName, false, 0));
            return room;
        }
    }

    public Question startGame(String code, String sessionId) {
        GameRoom room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            Player host = room.getPlayers().stream()
                    .filter(Player::isHost)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Room has no host."));
            if (!host.getId().equals(sessionId)) {
                throw new IllegalStateException("Only the host can start the game.");
            }
            if (room.getStatus() != GameStatus.LOBBY) {
                throw new IllegalStateException("Game has already started.");
            }
            room.setStatus(GameStatus.IN_PROGRESS);
            room.setCurrentQuestionIndex(0);
            room.getAnswers().clear();
            resetScores(room);
            return questions.get(0);
        }
    }

    public AnswerAckEvent submitAnswer(String code, String sessionId, int questionIndex, String selectedOption) {
        GameRoom room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            if (room.getStatus() != GameStatus.IN_PROGRESS) {
                throw new IllegalStateException("Game is not in progress.");
            }
            if (room.getCurrentQuestionIndex() != questionIndex) {
                throw new IllegalStateException("Question is no longer active.");
            }
            Player player = room.getPlayers().stream()
                    .filter(existingPlayer -> existingPlayer.getId().equals(sessionId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Player is not part of the room."));
            if (room.getAnswers().containsKey(sessionId)) {
                throw new IllegalStateException("Answer already submitted for this question.");
            }

            String normalizedOption = normalizeOption(selectedOption);
            Question question = questions.get(questionIndex);
            if (!question.options().containsKey(normalizedOption)) {
                throw new IllegalArgumentException("Selected option is invalid.");
            }

            room.getAnswers().put(sessionId, normalizedOption);
            boolean correct = question.correctOption().equalsIgnoreCase(normalizedOption);
            int points = correct ? CORRECT_ANSWER_POINTS : 0;
            if (correct) {
                player.setScore(player.getScore() + points);
            }
            String message = correct ? "Correct!" : "Incorrect.";
            return new AnswerAckEvent("ANSWER_ACK", correct, points, message);
        }
    }

    public Optional<GameRoom> removePlayer(String sessionId) {
        for (GameRoom room : rooms.values()) {
            synchronized (room) {
                boolean removed = room.getPlayers().removeIf(player -> player.getId().equals(sessionId));
                if (!removed) {
                    continue;
                }
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
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rooms.get(code.trim().toUpperCase(Locale.ROOT)));
    }

    public Optional<Question> advanceToNextQuestion(String code) {
        GameRoom room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            int nextIndex = room.getCurrentQuestionIndex() + 1;
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
        GameRoom room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            room.getStatus();
            room.setStatus(GameStatus.FINISHED);
            room.getAnswers().clear();
        }
    }

    public boolean haveAllPlayersAnswered(String code) {
        GameRoom room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            return !room.getPlayers().isEmpty() && room.getAnswers().size() >= room.getPlayers().size();
        }
    }

    public List<Player> getPlayersSortedByScore(String code) {
        GameRoom room = getRequiredRoom(normalizeRoomCode(code));
        synchronized (room) {
            return room.getPlayers().stream()
                    .sorted(Comparator.comparingInt(Player::getScore).reversed().thenComparing(Player::getName))
                    .map(player -> new Player(player.getId(), player.getName(), player.isHost(), player.getScore()))
                    .toList();
        }
    }

    public List<Question> getQuestions() {
        return questions;
    }

    private void resetScores(GameRoom room) {
        room.getPlayers().forEach(player -> player.setScore(0));
    }

    private void ensureSingleHost(GameRoom room) {
        boolean hasHost = room.getPlayers().stream().anyMatch(Player::isHost);
        if (!hasHost && !room.getPlayers().isEmpty()) {
            room.getPlayers().get(0).setHost(true);
        }
    }

    private GameRoom getRequiredRoom(String code) {
        GameRoom room = rooms.get(code);
        if (room == null) {
            throw new IllegalArgumentException("Room not found.");
        }
        return room;
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(ROOM_CODE_LENGTH);
            for (int index = 0; index < ROOM_CODE_LENGTH; index++) {
                builder.append(ROOM_CODE_ALPHABET.charAt(secureRandom.nextInt(ROOM_CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (rooms.containsKey(code));
        return code;
    }

    private String validatePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name is required.");
        }
        return playerName.trim();
    }

    private String normalizeRoomCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Room code is required.");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOption(String selectedOption) {
        if (selectedOption == null || selectedOption.isBlank()) {
            throw new IllegalArgumentException("Selected option is required.");
        }
        return selectedOption.trim().toUpperCase(Locale.ROOT);
    }

    private List<Question> buildQuestions() {
        return List.of(
                new Question("Which empire built Machu Picchu?", Map.of("A", "Aztec", "B", "Maya", "C", "Inca", "D", "Roman"), "C"),
                new Question("What planet is known as the Red Planet?", Map.of("A", "Mars", "B", "Venus", "C", "Mercury", "D", "Jupiter"), "A"),
                new Question("Which country has the city of Reykjavik as its capital?", Map.of("A", "Norway", "B", "Iceland", "C", "Finland", "D", "Sweden"), "B"),
                new Question("Who directed the 1997 film Titanic?", Map.of("A", "Steven Spielberg", "B", "Christopher Nolan", "C", "James Cameron", "D", "Ridley Scott"), "C"),
                new Question("What gas do plants primarily absorb for photosynthesis?", Map.of("A", "Oxygen", "B", "Hydrogen", "C", "Carbon Dioxide", "D", "Nitrogen"), "C"),
                new Question("The Battle of Hastings took place in which year?", Map.of("A", "1066", "B", "1215", "C", "1415", "D", "1666"), "A"),
                new Question("Which ocean is the largest on Earth?", Map.of("A", "Atlantic Ocean", "B", "Indian Ocean", "C", "Arctic Ocean", "D", "Pacific Ocean"), "D"),
                new Question("What is the chemical symbol for gold?", Map.of("A", "Ag", "B", "Au", "C", "Gd", "D", "Go"), "B"),
                new Question("Which artist released the album '1989'?", Map.of("A", "Taylor Swift", "B", "Adele", "C", "Beyoncé", "D", "Dua Lipa"), "A"),
                new Question("Which desert is the largest hot desert in the world?", Map.of("A", "Gobi", "B", "Kalahari", "C", "Sahara", "D", "Arabian"), "C")
        );
    }
}
