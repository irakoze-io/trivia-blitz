package io.github.irakoze.triviabl.dto;

public record SubmitAnswerRequest(String roomCode, int questionIndex, String selectedOption) {
}
