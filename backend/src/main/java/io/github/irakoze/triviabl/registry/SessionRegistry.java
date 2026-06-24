package io.github.irakoze.triviabl.registry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class SessionRegistry {

    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    public void register(String sessionId, String roomCode) {
        sessionToRoom.put(sessionId, roomCode);
    }

    public void unregister(String sessionId) {
        sessionToRoom.remove(sessionId);
    }

    public Optional<String> getRoomCode(String sessionId) {
        return Optional.ofNullable(sessionToRoom.get(sessionId));
    }
}
