import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { AppComponent } from './app.component';
import { HomeComponent } from './components/home/home.component';
import { LeaderboardComponent } from './components/leaderboard/leaderboard.component';
import { LobbyComponent } from './components/lobby/lobby.component';
import { QuestionComponent } from './components/question/question.component';
import { GameStompService } from './services/game-stomp.service';
import { GameStore } from './services/game-store.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [AppComponent, HomeComponent, LobbyComponent, QuestionComponent, LeaderboardComponent],
      providers: [
        {
          provide: GameStore,
          useValue: {
            state$: of({
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
            }),
          },
        },
        {
          provide: GameStompService,
          useValue: {
            connect: () => of(3),
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
