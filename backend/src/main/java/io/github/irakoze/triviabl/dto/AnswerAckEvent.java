package io.github.irakoze.triviabl.dto;

public record AnswerAckEvent(String type, boolean correct, int points, String message) {
}
