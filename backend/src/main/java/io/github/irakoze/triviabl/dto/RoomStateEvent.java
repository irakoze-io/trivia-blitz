package io.github.irakoze.triviabl.dto;

import java.util.List;

import io.github.irakoze.triviabl.model.GameStatus;

public record RoomStateEvent(String type, String roomCode, List<PlayerView> players, GameStatus status) {
}
