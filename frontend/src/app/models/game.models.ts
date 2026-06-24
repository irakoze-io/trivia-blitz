export interface Player {
  name: string;
  isHost: boolean;
  score: number;
}

export type GameStatus = 'LOBBY' | 'IN_PROGRESS' | 'FINISHED';
export type EventType =
  | 'ROOM_STATE'
  | 'QUESTION'
  | 'TICK'
  | 'LEADERBOARD'
  | 'GAME_OVER'
  | 'ANSWER_ACK'
  | 'ERROR';

export interface RoomStateEvent {
  type: 'ROOM_STATE';
  roomCode: string;
  players: Player[];
  status: GameStatus;
}

export interface QuestionEvent {
  type: 'QUESTION';
  questionIndex: number;
  text: string;
  options: Record<string, string>;
  timeLimit: number;
}

export interface TickEvent {
  type: 'TICK';
  roomCode: string;
  secondsLeft: number;
}

export interface LeaderboardEvent {
  type: 'LEADERBOARD';
  scores: { playerName: string; score: number }[];
  isFinal: boolean;
}

export interface GameOverEvent {
  type: 'GAME_OVER';
  scores: { playerName: string; score: number }[];
}

export interface AnswerAckEvent {
  type: 'ANSWER_ACK';
  correct: boolean;
  points: number;
  message: string;
}

export interface ErrorEvent {
  type: 'ERROR';
  message: string;
}

export type GameEvent =
  | RoomStateEvent
  | QuestionEvent
  | TickEvent
  | LeaderboardEvent
  | GameOverEvent
  | AnswerAckEvent
  | ErrorEvent;
