package io.github.irakoze.triviabl.dto;

public record TickEvent(String type, String roomCode, int secondsLeft) {
}
