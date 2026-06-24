import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp, RxStompState } from '@stomp/rx-stomp';
import SockJS from 'sockjs-client';
import { map, Observable } from 'rxjs';

import { GameEvent } from '../models/game.models';

@Injectable()
export class GameStompService implements OnDestroy {
  private readonly rxStomp = new RxStomp();

  constructor() {
    this.rxStomp.configure({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      connectHeaders: {},
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => undefined,
    });
  }

  connect(): Observable<RxStompState> {
    if (!this.rxStomp.active) {
      this.rxStomp.activate();
    }

    return this.rxStomp.connectionState$;
  }

  get connectionState$(): Observable<RxStompState> {
    return this.rxStomp.connectionState$;
  }

  subscribeToRoom(roomCode: string): Observable<GameEvent> {
    return this.rxStomp.watch(`/topic/room/${roomCode}`).pipe(
      map((message) => JSON.parse(message.body) as GameEvent),
    );
  }

  subscribeToAck(): Observable<GameEvent> {
    return this.rxStomp.watch('/user/queue/ack').pipe(
      map((message) => JSON.parse(message.body) as GameEvent),
    );
  }

  createRoom(playerName: string): void {
    this.publish('/app/room/create', { playerName });
  }

  joinRoom(roomCode: string, playerName: string): void {
    this.publish('/app/room/join', { roomCode, playerName });
  }

  startGame(roomCode: string): void {
    this.publish('/app/room/start', { roomCode });
  }

  submitAnswer(roomCode: string, questionIndex: number, selectedOption: string): void {
    this.publish('/app/answer/submit', { roomCode, questionIndex, selectedOption });
  }

  disconnect(): void {
    void this.rxStomp.deactivate();
  }

  ngOnDestroy(): void {
    this.disconnect();
  }

  private publish(destination: string, body: object): void {
    this.rxStomp.publish({
      destination,
      body: JSON.stringify(body),
    });
  }
}
