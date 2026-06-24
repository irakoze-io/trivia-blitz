import { Component } from '@angular/core';
import { RxStompState } from '@stomp/rx-stomp';
import { map, Observable } from 'rxjs';

import { GameStompService } from './services/game-stomp.service';
import { GameStore, GameState } from './services/game-store.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  readonly state$: Observable<GameState> = this.gameStore.state$;
  readonly connectionStatus$ = this.stompService.connect().pipe(
    map((state) => this.mapConnectionState(state)),
  );

  constructor(
    private readonly gameStore: GameStore,
    private readonly stompService: GameStompService,
  ) {}

  private mapConnectionState(state: RxStompState): string {
    switch (state) {
      case RxStompState.OPEN:
        return 'Connected';
      case RxStompState.CONNECTING:
        return 'Connecting';
      case RxStompState.CLOSING:
        return 'Disconnecting';
      case RxStompState.CLOSED:
      default:
        return 'Offline';
    }
  }
}
