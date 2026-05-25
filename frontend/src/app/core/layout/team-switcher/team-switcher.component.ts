import { Component, input, output } from '@angular/core';
import { MatSelectModule } from '@angular/material/select';
import { type Team } from '../../../shared/models';

@Component({
  selector: 'tf-team-switcher',
  standalone: true,
  imports: [MatSelectModule],
  template: `
    <mat-select
      [value]="activeTeamId()"
      (valueChange)="teamChange.emit($event)"
      aria-label="Switch team"
    >
      @for (team of teams(); track team.id) {
        <mat-option [value]="team.id">{{ team.name }}</mat-option>
      }
    </mat-select>
  `,
})
export class TeamSwitcherComponent {
  readonly teams = input<readonly Team[]>([]);
  readonly activeTeamId = input<string | null>(null);
  readonly teamChange = output<string>();
}
