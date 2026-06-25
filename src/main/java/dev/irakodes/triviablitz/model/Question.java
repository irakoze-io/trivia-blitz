package dev.irakodes.triviablitz.model;

import java.util.Map;

public record Question(
        String id, String category, String question,
        Map<String, String> options, String correctAnswer
) {
}
