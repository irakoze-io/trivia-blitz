package io.github.irakoze.triviabl.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.irakoze.triviabl.dto.AnswerAckEvent;
import io.github.irakoze.triviabl.model.GameRoom;
import io.github.irakoze.triviabl.model.GameStatus;

class GameServiceTest {

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService();
    }

    @Test
    void createRoomCreatesLobbyWithHost() {
        GameRoom room = gameService.createRoom("Host Player", "session-1");

        assertNotNull(room.getCode());
        assertEquals(6, room.getCode().length());
        assertEquals(GameStatus.LOBBY, room.getStatus());
        assertEquals(1, room.getPlayers().size());
        assertEquals("Host Player", room.getPlayers().get(0).getName());
        assertTrue(room.getPlayers().get(0).isHost());
        assertTrue(gameService.getRoom(room.getCode()).isPresent());
    }

    @Test
    void joinRoomAddsSecondPlayer() {
        GameRoom room = gameService.createRoom("Host", "session-1");

        GameRoom updatedRoom = gameService.joinRoom(room.getCode(), "Guest", "session-2");

        assertEquals(2, updatedRoom.getPlayers().size());
        assertTrue(updatedRoom.getPlayers().stream().anyMatch(player -> player.getName().equals("Guest")));
    }

    @Test
    void submitAnswerAwardsPointsForCorrectSelection() {
        GameRoom room = gameService.createRoom("Host", "session-1");
        gameService.startGame(room.getCode(), "session-1");

        AnswerAckEvent ackEvent = gameService.submitAnswer(room.getCode(), "session-1", 0, "C");

        assertTrue(ackEvent.correct());
        assertEquals(100, ackEvent.points());
        assertEquals(100, room.getPlayers().get(0).getScore());
        assertEquals("C", room.getAnswers().get("session-1"));
    }

    @Test
    void removePlayerDeletesEmptyRoom() {
        GameRoom room = gameService.createRoom("Host", "session-1");

        assertTrue(gameService.removePlayer("session-1").isEmpty());
        assertFalse(gameService.getRoom(room.getCode()).isPresent());
    }

    @Test
    void joinRoomRejectsStartedGame() {
        GameRoom room = gameService.createRoom("Host", "session-1");
        gameService.startGame(room.getCode(), "session-1");

        assertThrows(IllegalStateException.class,
                () -> gameService.joinRoom(room.getCode(), "Guest", "session-2"));
    }
}
