package dev.irakodes.triviablitz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRoom {
    private String code;
    private GameStatus status;
    private List<Player> players;
    private int currentQuestionIndex;
    private Map<String, String> answers;
}
