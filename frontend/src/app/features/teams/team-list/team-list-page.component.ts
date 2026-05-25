import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { TeamsStore } from '../teams.store';
import { CreateTeamDialogComponent } from '../components/create-team-dialog/create-team-dialog.component';
import { type CreateTeamRequest } from '../../../shared/models';

@Component({
  selector: 'tf-team-list-page',
  standalone: true,
  imports: [MatButtonModule, MatListModule, MatProgressSpinnerModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1>Teams</h1>
      <button mat-flat-button color="primary" (click)="openCreateDialog()">
        <mat-icon>add</mat-icon>
        Create Team
      </button>
    </div>

    @if (store.loading()) {
      <mat-spinner diameter="40" />
    } @else if (store.error()) {
      <p role="alert" class="error">{{ store.error() }}</p>
    } @else {
      <mat-list>
        @for (team of store.teams(); track team.id) {
          <mat-list-item button (click)="navigateToTeam(team.id)">
            <span matListItemTitle>{{ team.name }}</span>
            @if (team.description) {
              <span matListItemLine>{{ team.description }}</span>
            }
          </mat-list-item>
        } @empty {
          <p class="empty-state">No teams yet. Create one to get started.</p>
        }
      </mat-list>
    }
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; padding: 16px; }
    .error { color: var(--mat-sys-error, red); padding: 16px; }
    .empty-state { padding: 16px; color: var(--mat-sys-on-surface-variant, #666); }
  `],
})
export class TeamListPageComponent implements OnInit {
  protected readonly store = inject(TeamsStore);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  ngOnInit(): void {
    void this.store.loadTeams();
  }

  protected navigateToTeam(teamId: string): void {
    void this.router.navigate(['/teams', teamId]);
  }

  protected openCreateDialog(): void {
    const ref = this.dialog.open(CreateTeamDialogComponent, { width: '400px' });
    ref.afterClosed().subscribe((result: CreateTeamRequest | undefined) => {
      if (result) {
        void this.store.createTeam(result);
      }
    });
  }
}
