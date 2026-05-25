import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { ProjectsStore } from '../projects.store';
import { CreateProjectDialogComponent } from '../create-project-dialog/create-project-dialog.component';
import { type CreateProjectRequest } from '../../../shared/models';

@Component({
  selector: 'tf-project-list-page',
  standalone: true,
  imports: [MatButtonModule, MatListModule, MatProgressSpinnerModule, MatIconModule],
  template: `
    <div class="page-header">
      <h2>Projects</h2>
      <button mat-flat-button color="primary" (click)="openCreateDialog()">
        <mat-icon>add</mat-icon>
        Create Project
      </button>
    </div>

    @if (store.loading()) {
      <mat-spinner diameter="40" />
    } @else if (store.error()) {
      <p role="alert" class="error">{{ store.error() }}</p>
    } @else {
      <mat-list>
        @for (project of store.projects(); track project.id) {
          <mat-list-item>
            <span matListItemTitle>{{ project.name }}</span>
            @if (project.description) {
              <span matListItemLine>{{ project.description }}</span>
            }
          </mat-list-item>
        } @empty {
          <p class="empty-state">No projects yet.</p>
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
export class ProjectListPageComponent implements OnInit {
  protected readonly store = inject(ProjectsStore);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  ngOnInit(): void {
    const teamId = this.teamId();
    if (teamId) {
      void this.store.loadProjects(teamId);
    }
  }

  private teamId(): string | null {
    return this.route.snapshot.paramMap.get('teamId');
  }

  protected openCreateDialog(): void {
    const teamId = this.teamId();
    if (!teamId) return;
    const ref = this.dialog.open(CreateProjectDialogComponent, { width: '400px', data: { teamId } });
    ref.afterClosed().subscribe((result: CreateProjectRequest | undefined) => {
      if (result) {
        void this.store.createProject(teamId, result);
      }
    });
  }
}
