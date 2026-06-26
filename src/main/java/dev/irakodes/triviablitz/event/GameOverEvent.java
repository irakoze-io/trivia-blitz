package dev.irakodes.triviablitz.event;

import dev.irakodes.triviablitz.dto.ScoreEntry;

import java.util.List;

public record GameOverEvent(String type, List<ScoreEntry> scores) {}
