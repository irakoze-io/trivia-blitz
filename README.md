# Trivia Blitz

A real-time multiplayer trivia game that demonstrates the most important WebSocket patterns: rooms, broadcast, user-specific messages, and timed server-push events.

## Architecture

| Layer | Technology |
|---|---|
| Backend | Spring Boot 3.3 · STOMP over SockJS · Java 17 |
| Frontend | Angular 17 · RxStomp · BehaviorSubject store |
| Transport | WebSocket with SockJS fallback |

## WebSocket concepts covered

| Concept | Where |
|---|---|
| Topic broadcast | Leaderboard, question push, countdown ticks |
| User queue (private) | Answer ACK, error messages |
| Server-push (no client request) | The countdown ticker (`TICK` every second) |
| Connection lifecycle | Player disconnect removes them from the room |
| STOMP over SockJS | Transport layer with fallback |

---

## Backend (`backend/`)

### Key components

| Class | Responsibility |
|---|---|
| `WebSocketConfig` | Registers `/ws` SockJS endpoint; sets `/app`, `/topic`, `/user` prefixes |
| `GameRoomController` | STOMP message handlers for `/room/create`, `/room/join`, `/room/start`, `/answer/submit` |
| `GameService` | Room lifecycle (LOBBY → IN_PROGRESS → FINISHED), scoring, question sequencing |
| `CountdownService` | `TaskScheduler`-driven per-room countdown, emits `TICK` events every second |
| `SessionRegistry` | Maps STOMP session IDs to room codes |
| `WebSocketEventListener` | Handles `SessionDisconnectEvent` — removes player, rebroadcasts room state |

### STOMP API

**Client → Server** (prefix `/app`):

| Destination | Body |
|---|---|
| `/app/room/create` | `{ "playerName": "Alice" }` |
| `/app/room/join` | `{ "roomCode": "ABC123", "playerName": "Bob" }` |
| `/app/room/start` | `{ "roomCode": "ABC123" }` (host only) |
| `/app/answer/submit` | `{ "roomCode": "ABC123", "questionIndex": 0, "selectedOption": "B" }` |

**Server → All players** (`/topic/room/{code}`):

| Event type | Description |
|---|---|
| `ROOM_STATE` | Player list + lobby status update |
| `QUESTION` | Question text, options A–D, time limit |
| `TICK` | Seconds remaining on the current question |
| `LEADERBOARD` | Ranked scores after each question |
| `GAME_OVER` | Final ranked scores |

**Server → Individual player** (`/user/queue/ack`):

| Event type | Description |
|---|---|
| `ANSWER_ACK` | Whether the answer was correct + points awarded |
| `ERROR` | Validation or game-state error message |

### Running the backend

```bash
cd backend
./mvnw spring-boot:run
# Server starts on http://localhost:8080
```

### Running backend tests

```bash
cd backend
./mvnw test
```

---

## Frontend (`frontend/`)

### Key components

| Class | Responsibility |
|---|---|
| `GameStompService` | Wraps `RxStomp`; exposes typed `Observable<GameEvent>` subscriptions and publish helpers |
| `GameStore` | `BehaviorSubject<GameState>` — single source of truth for all views |
| `HomeComponent` | Enter name, create or join a room |
| `LobbyComponent` | Player list with host indicator, "Start Game" button (host only) |
| `QuestionComponent` | Question + A/B/C/D buttons, live countdown, ACK feedback |
| `LeaderboardComponent` | Ranked scores after each question; game-over screen |

The `AppComponent` drives view switching by reading `state.view` from the `GameStore` — no Angular Router needed.

### Running the frontend

```bash
cd frontend
npm install
npx ng serve
# App opens at http://localhost:4200
```

> The backend must be running on `localhost:8080` before opening the app.

---

## Game flow

```
Host creates room  →  Room code displayed (share it!)
Players join       →  Lobby shows live player list
Host starts game   →  First question pushed to all players
                       Countdown ticks every second (server-push)
Players answer     →  Private ACK (correct/incorrect + points)
                       When all answered (or time expires) →
                         Leaderboard broadcast to all
                         Next question pushed (or GAME_OVER)
```
