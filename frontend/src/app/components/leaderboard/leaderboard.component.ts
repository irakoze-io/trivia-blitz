import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-leaderboard',
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.scss'],
})
export class LeaderboardComponent {
  @Input({ required: true }) scores: { playerName: string; score: number }[] = [];
  @Input() isFinal = false;
}
