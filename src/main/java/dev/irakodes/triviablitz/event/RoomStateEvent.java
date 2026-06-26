package dev.irakodes.triviablitz.event;

import dev.irakodes.triviablitz.dto.PlayerView;
import dev.irakodes.triviablitz.model.GameStatus;

import java.util.List;

public record RoomStateEvent(String type, String roomCode, List<PlayerView> players,
                             GameStatus status) {
}
