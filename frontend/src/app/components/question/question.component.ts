import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';

import { GameStompService } from '../../services/game-stomp.service';
import { GameState } from '../../services/game-store.service';

@Component({
  selector: 'app-question',
  templateUrl: './question.component.html',
  styleUrls: ['./question.component.scss'],
})
export class QuestionComponent implements OnChanges {
  @Input({ required: true }) state!: GameState;

  selectedOption: string | null = null;
  hasSubmitted = false;

  constructor(private readonly stompService: GameStompService) {}

  ngOnChanges(changes: SimpleChanges): void {
    const currentQuestion = changes['state']?.currentValue?.currentQuestion as GameState['currentQuestion'] | undefined;
    const previousQuestion = changes['state']?.previousValue?.currentQuestion as GameState['currentQuestion'] | undefined;

    if (currentQuestion?.questionIndex !== previousQuestion?.questionIndex) {
      this.selectedOption = null;
      this.hasSubmitted = false;
    }
  }

  get optionKeys(): string[] {
    return this.state.currentQuestion ? Object.keys(this.state.currentQuestion.options) : [];
  }

  submit(optionKey: string): void {
    if (this.hasSubmitted || !this.state.currentQuestion || !this.state.roomCode) {
      return;
    }

    this.selectedOption = optionKey;
    this.hasSubmitted = true;
    this.stompService.submitAnswer(
      this.state.roomCode,
      this.state.currentQuestion.questionIndex,
      optionKey,
    );
  }

  buttonClass(optionKey: string): string {
    if (!this.state.lastAck || this.selectedOption !== optionKey) {
      return '';
    }

    return this.state.lastAck.correct ? 'correct' : 'incorrect';
  }
}
