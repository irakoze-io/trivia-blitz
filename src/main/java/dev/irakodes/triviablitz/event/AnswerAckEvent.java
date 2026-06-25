package dev.irakodes.triviablitz.event;

public record AnswerAckEvent (String type, boolean correct, int points, String message) {
}
