import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CdkDropListGroup } from '@angular/cdk/drag-drop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { BoardStore, BOARD_COLUMNS } from '../board.store';
import { BoardColumnComponent } from '../components/board-column/board-column.component';
import { BoardFiltersComponent } from '../components/board-filters/board-filters.component';
import { TaskDetailPanelComponent } from '../../tasks/task-detail-panel/task-detail-panel.component';
import { type BoardFilters } from '../board.store';
import { TaskStatus } from '../../../shared/models/task-status';

@Component({
  selector: 'tf-board-page',
  standalone: true,
  imports: [
    CdkDropListGroup,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatButtonModule,
    MatDialogModule,
    BoardColumnComponent,
    BoardFiltersComponent,
    TaskDetailPanelComponent,
  ],
  template: `
    <div class="board-page">
      <div class="board-page__toolbar">
        <h2 class="board-page__title">Board</h2>
        <tf-board-filters
          (filtersChanged)="onFiltersChanged($event)"
        />
      </div>

      @if (store.loading()) {
        <div class="board-page__loading">
          <mat-spinner diameter="40" />
        </div>
      } @else {
        <div class="board-page__board" cdkDropListGroup>
          @for (column of columns; track column) {
            <tf-board-column
              [status]="column"
              [tasks]="store.tasksByStatus()[column]"
              (taskMoved)="onTaskMoved($event)"
              (taskClicked)="selectedTaskId.set($event.id)"
            />
          }
        </div>
      }

      @if (store.error()) {
        <div class="board-page__error" role="alert">{{ store.error() }}</div>
      }

      <tf-task-detail-panel
        [taskId]="selectedTaskId()"
        (panelClosed)="selectedTaskId.set(null)"
      />
    </div>
  `,
  styles: [
    `
      .board-page {
        display: flex;
        flex-direction: column;
        height: 100%;
        padding: 16px;
        overflow: hidden;
      }
      .board-page__toolbar {
        display: flex;
        align-items: center;
        gap: 16px;
        margin-bottom: 16px;
        flex-wrap: wrap;
      }
      .board-page__title {
        font-size: 1.25rem;
        font-weight: 600;
        margin: 0;
        flex-shrink: 0;
      }
      .board-page__loading {
        display: flex;
        justify-content: center;
        align-items: center;
        flex: 1;
      }
      .board-page__board {
        display: flex;
        gap: 12px;
        overflow-x: auto;
        flex: 1;
        align-items: flex-start;
        padding-bottom: 8px;
      }
      .board-page__error {
        color: #c62828;
        padding: 8px;
        font-size: 0.875rem;
      }
    `,
  ],
})
export class BoardPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  protected readonly store = inject(BoardStore);

  protected readonly columns = BOARD_COLUMNS;
  protected readonly selectedTaskId = signal<string | null>(null);

  ngOnInit(): void {
    const projectId = this.route.snapshot.paramMap.get('projectId');
    if (projectId) {
      void this.store.loadTasks(projectId);
    }
  }

  protected onTaskMoved(event: { taskId: string; from: TaskStatus; to: TaskStatus }): void {
    this.store.moveTask(event.taskId, event.from, event.to);
  }

  protected onFiltersChanged(filters: BoardFilters): void {
    this.store.setFilters(filters);
  }
}
