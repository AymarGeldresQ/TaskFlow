import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { type TeamMember } from '../../../../shared/models/team.model';
import { type Label } from '../../../../shared/models/label.model';
import { TaskStatus } from '../../../../shared/models/task-status';
import { type BoardFilters } from '../../board.store';

const BOARD_STATUS_OPTIONS: TaskStatus[] = [
  TaskStatus.BACKLOG,
  TaskStatus.TODO,
  TaskStatus.IN_PROGRESS,
  TaskStatus.IN_REVIEW,
  TaskStatus.DONE,
];

@Component({
  selector: 'tf-board-filters',
  standalone: true,
  imports: [FormsModule, MatSelectModule, MatFormFieldModule, MatButtonModule],
  template: `
    <div class="board-filters">
      <mat-form-field appearance="outline" class="board-filters__field">
        <mat-label>Status</mat-label>
        <mat-select
          multiple
          [(ngModel)]="selectedStatuses"
          (ngModelChange)="emitFilters()"
        >
          @for (status of statusOptions; track status) {
            <mat-option [value]="status">{{ status }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="board-filters__field">
        <mat-label>Assignee</mat-label>
        <mat-select
          multiple
          [(ngModel)]="selectedAssignees"
          (ngModelChange)="emitFilters()"
        >
          @for (member of members(); track member.userId) {
            <mat-option [value]="member.userId">{{ member.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="board-filters__field">
        <mat-label>Label</mat-label>
        <mat-select
          multiple
          [(ngModel)]="selectedLabels"
          (ngModelChange)="emitFilters()"
        >
          @for (label of labels(); track label.id) {
            <mat-option [value]="label.id">{{ label.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      @if (hasActiveFilters()) {
        <button mat-button color="warn" (click)="clearFilters()">Clear filters</button>
      }
    </div>
  `,
  styles: [
    `
      .board-filters {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 8px 0;
        flex-wrap: wrap;
      }
      .board-filters__field {
        min-width: 160px;
      }
    `,
  ],
})
export class BoardFiltersComponent {
  readonly members = input<TeamMember[]>([]);
  readonly labels = input<Label[]>([]);
  readonly filtersChanged = output<BoardFilters>();

  protected readonly statusOptions = BOARD_STATUS_OPTIONS;

  protected selectedStatuses: TaskStatus[] = [];
  protected selectedAssignees: string[] = [];
  protected selectedLabels: string[] = [];

  protected hasActiveFilters(): boolean {
    return (
      this.selectedStatuses.length > 0 ||
      this.selectedAssignees.length > 0 ||
      this.selectedLabels.length > 0
    );
  }

  protected emitFilters(): void {
    this.filtersChanged.emit({
      statuses: this.selectedStatuses,
      assigneeIds: this.selectedAssignees,
      labelIds: this.selectedLabels,
    });
  }

  protected clearFilters(): void {
    this.selectedStatuses = [];
    this.selectedAssignees = [];
    this.selectedLabels = [];
    this.emitFilters();
  }
}
