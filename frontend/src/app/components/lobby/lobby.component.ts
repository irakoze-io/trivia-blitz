import { Component, Input } from '@angular/core';

import { GameStompService } from '../../services/game-stomp.service';
import { GameState } from '../../services/game-store.service';

@Component({
  selector: 'app-lobby',
  templateUrl: './lobby.component.html',
  styleUrls: ['./lobby.component.scss'],
})
export class LobbyComponent {
  @Input({ required: true }) state!: GameState;

  constructor(private readonly stompService: GameStompService) {}

  startGame(): void {
    if (!this.state.roomCode) {
      return;
    }

    this.stompService.startGame(this.state.roomCode);
  }
}
