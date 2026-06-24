import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import {
  AnswerAckEvent,
  GameEvent,
  GameStatus,
  Player,
  QuestionEvent,
} from '../models/game.models';

export interface GameState {
  playerName: string;
  roomCode: string;
  players: Player[];
  isHost: boolean;
  status: GameStatus;
  currentQuestion: QuestionEvent | null;
  secondsLeft: number;
  leaderboard: { playerName: string; score: number }[];
  lastAck: AnswerAckEvent | null;
  error: string | null;
  view: 'home' | 'lobby' | 'question' | 'leaderboard' | 'gameover';
}

export const initialState: GameState = {
  playerName: '',
  roomCode: '',
  players: [],
  isHost: false,
  status: 'LOBBY',
  currentQuestion: null,
  secondsLeft: 0,
  leaderboard: [],
  lastAck: null,
  error: null,
  view: 'home',
};

@Injectable()
export class GameStore {
  private readonly stateSubject = new BehaviorSubject<GameState>(initialState);
  readonly state$: Observable<GameState> = this.stateSubject.asObservable();

  handleEvent(event: GameEvent): void {
    const current = this.stateSubject.value;

    switch (event.type) {
      case 'ROOM_STATE':
        this.stateSubject.next({
          ...current,
          roomCode: event.roomCode,
          players: event.players,
          isHost: event.players.some(
            (player) => player.name === current.playerName && player.isHost,
          ),
          status: event.status,
          error: null,
          view: event.status === 'LOBBY' ? 'lobby' : current.view,
        });
        return;
      case 'QUESTION':
        this.stateSubject.next({
          ...current,
          currentQuestion: event,
          secondsLeft: event.timeLimit,
          lastAck: null,
          error: null,
          status: 'IN_PROGRESS',
          view: 'question',
        });
        return;
      case 'TICK':
        this.patchState({ secondsLeft: event.secondsLeft });
        return;
      case 'LEADERBOARD':
        this.stateSubject.next({
          ...current,
          leaderboard: event.scores,
          currentQuestion: null,
          secondsLeft: 0,
          status: event.isFinal ? 'FINISHED' : current.status,
          view: event.isFinal ? 'gameover' : 'leaderboard',
        });
        return;
      case 'GAME_OVER':
        this.stateSubject.next({
          ...current,
          leaderboard: event.scores,
          currentQuestion: null,
          secondsLeft: 0,
          status: 'FINISHED',
          view: 'gameover',
        });
        return;
      case 'ANSWER_ACK':
        this.patchState({
          lastAck: event,
          error: null,
        });
        return;
      case 'ERROR':
        this.patchState({
          error: event.message,
        });
        return;
    }
  }

  setPlayerName(playerName: string): void {
    this.patchState({ playerName, error: null });
  }

  setRoomCode(roomCode: string): void {
    this.patchState({ roomCode: roomCode.trim().toUpperCase(), error: null });
  }

  reset(): void {
    this.stateSubject.next(initialState);
  }

  private patchState(patch: Partial<GameState>): void {
    this.stateSubject.next({
      ...this.stateSubject.value,
      ...patch,
    });
  }
}
