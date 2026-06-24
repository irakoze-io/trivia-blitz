import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppComponent } from './app.component';
import { HomeComponent } from './components/home/home.component';
import { LeaderboardComponent } from './components/leaderboard/leaderboard.component';
import { LobbyComponent } from './components/lobby/lobby.component';
import { QuestionComponent } from './components/question/question.component';
import { GameStompService } from './services/game-stomp.service';
import { GameStore } from './services/game-store.service';

function initializeApp(gameStompService: GameStompService): () => Promise<void> {
  return () => {
    gameStompService.connect().subscribe();
    return Promise.resolve();
  };
}

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    LobbyComponent,
    QuestionComponent,
    LeaderboardComponent,
  ],
  imports: [BrowserModule, FormsModule, ReactiveFormsModule],
  providers: [
    GameStompService,
    GameStore,
    {
      provide: APP_INITIALIZER,
      useFactory: initializeApp,
      deps: [GameStompService],
      multi: true,
    },
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
