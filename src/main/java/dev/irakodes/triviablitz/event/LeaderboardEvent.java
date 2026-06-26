package dev.irakodes.triviablitz.event;

import dev.irakodes.triviablitz.dto.ScoreEntry;

import java.util.List;

public record LeaderboardEvent(
        String type,
        List<ScoreEntry> scores,
        boolean isFinal
) {}
