package io.github.irakoze.triviabl.model;

import java.util.Map;

public record Question(String text, Map<String, String> options, String correctOption) {
}
