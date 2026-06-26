package dev.irakodes.triviablitz.event;

public record TickEvent(String type, String roomCode, int secondsLeft) {
}
