package dev.irakodes.triviablitz.event;

import java.util.Map;

public record QuestionEvent (
   String type,
   int questionIndex,
   String text,
   Map<String, String> options,
   int timeLimit
) {}
