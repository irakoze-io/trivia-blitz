package io.github.irakoze.triviabl.model;

import java.util.List;
import java.util.Map;

public class GameRoom {

    private String code;
    private GameStatus status;
    private List<Player> players;
    private int currentQuestionIndex;
    private Map<String, String> answers;

    public GameRoom(String code, GameStatus status, List<Player> players, int currentQuestionIndex, Map<String, String> answers) {
        this.code = code;
        this.status = status;
        this.players = players;
        this.currentQuestionIndex = currentQuestionIndex;
        this.answers = answers;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int currentQuestionIndex) {
        this.currentQuestionIndex = currentQuestionIndex;
    }

    public Map<String, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, String> answers) {
        this.answers = answers;
    }
}
