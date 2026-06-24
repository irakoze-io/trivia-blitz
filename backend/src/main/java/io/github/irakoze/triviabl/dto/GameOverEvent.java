package io.github.irakoze.triviabl.dto;

import java.util.List;

public record GameOverEvent(String type, List<ScoreEntry> scores) {
}
