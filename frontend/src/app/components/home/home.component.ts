import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';

import { RoomStateEvent } from '../../models/game.models';
import { GameStompService } from '../../services/game-stomp.service';
import { GameStore, GameState, initialState } from '../../services/game-store.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit, OnDestroy {
  readonly form = this.formBuilder.group({
    playerName: ['', [Validators.required, Validators.maxLength(24)]],
    roomCode: ['', [Validators.maxLength(12)]],
  });

  state: GameState = initialState;

  private readonly subscriptions = new Subscription();
  private roomSubscription?: Subscription;
  private ackSubscription?: Subscription;
  private currentRoomTarget = '';

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly stompService: GameStompService,
    private readonly gameStore: GameStore,
  ) {}

  ngOnInit(): void {
    this.subscriptions.add(
      this.gameStore.state$.subscribe((state) => {
        this.state = state;
      }),
    );
  }

  createRoom(): void {
    const playerName = this.form.controls.playerName.value?.trim() ?? '';
    if (!playerName) {
      this.form.controls.playerName.markAsTouched();
      return;
    }

    this.gameStore.reset();
    this.gameStore.setPlayerName(playerName);
    this.ensureAckSubscription();
    this.subscribeToRoom('*');
    this.stompService.createRoom(playerName);
  }

  joinRoom(): void {
    const playerName = this.form.controls.playerName.value?.trim() ?? '';
    const roomCode = this.form.controls.roomCode.value?.trim().toUpperCase() ?? '';

    if (!playerName) {
      this.form.controls.playerName.markAsTouched();
    }

    if (!roomCode) {
      this.form.controls.roomCode.markAsTouched();
    }

    if (!playerName || !roomCode) {
      return;
    }

    this.gameStore.reset();
    this.gameStore.setPlayerName(playerName);
    this.gameStore.setRoomCode(roomCode);
    this.ensureAckSubscription();
    this.subscribeToRoom(roomCode);
    this.stompService.joinRoom(roomCode, playerName);
  }

  ngOnDestroy(): void {
    this.roomSubscription?.unsubscribe();
    this.subscriptions.unsubscribe();
  }

  private ensureAckSubscription(): void {
    if (this.ackSubscription) {
      return;
    }

    this.ackSubscription = this.stompService.subscribeToAck().subscribe((event) => {
      this.gameStore.handleEvent(event);
    });
    this.subscriptions.add(this.ackSubscription);
  }

  private subscribeToRoom(targetRoom: string): void {
    if (this.currentRoomTarget === targetRoom) {
      return;
    }

    this.roomSubscription?.unsubscribe();
    this.currentRoomTarget = targetRoom;
    this.roomSubscription = this.stompService.subscribeToRoom(targetRoom).subscribe((event) => {
      if (event.type === 'ROOM_STATE' && !this.isRelevantRoomState(event)) {
        return;
      }

      if (event.type === 'ROOM_STATE') {
        const roomEvent = event as RoomStateEvent;
        if (this.currentRoomTarget === '*' && roomEvent.roomCode) {
          this.gameStore.setRoomCode(roomEvent.roomCode);
          this.subscribeToRoom(roomEvent.roomCode);
        }
      }

      this.gameStore.handleEvent(event);
    });
    this.subscriptions.add(this.roomSubscription);
  }

  private isRelevantRoomState(event: RoomStateEvent): boolean {
    if (this.currentRoomTarget !== '*' && event.roomCode !== this.currentRoomTarget) {
      return false;
    }

    if (this.currentRoomTarget === '*') {
      return event.players.some((player) => player.name === this.state.playerName);
    }

    return true;
  }
}
