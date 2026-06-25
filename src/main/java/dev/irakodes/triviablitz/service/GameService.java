package dev.irakodes.triviablitz.service;

import dev.irakodes.triviablitz.model.GameRoom;
import dev.irakodes.triviablitz.model.GameStatus;
import dev.irakodes.triviablitz.model.Player;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private static final String ROOM_CODE_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private static final int ROOM_CODE_LENGTH = 6;

    private static final int CORRECT_ANSWER_POINTS = 100;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();

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
