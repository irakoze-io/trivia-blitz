package io.github.irakoze.triviabl.dto;

import java.util.List;

public record LeaderboardEvent(String type, List<ScoreEntry> scores, boolean isFinal) {
}
