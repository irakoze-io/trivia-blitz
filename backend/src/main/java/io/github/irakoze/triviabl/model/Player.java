package io.github.irakoze.triviabl.model;

public class Player {

    private String id;
    private String name;
    private boolean host;
    private int score;

    public Player(String id, String name, boolean host, int score) {
        this.id = id;
        this.name = name;
        this.host = host;
        this.score = score;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
