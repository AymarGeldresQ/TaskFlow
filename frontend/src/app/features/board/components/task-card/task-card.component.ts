import { Component, input, output } from '@angular/core';
import { CdkDrag, CdkDragPlaceholder } from '@angular/cdk/drag-drop';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PriorityChipComponent } from '../../../../shared/ui/priority-chip/priority-chip.component';
import { type Task } from '../../../../shared/models';
import { TaskStatus } from '../../../../shared/models/task-status';

@Component({
  selector: 'tf-task-card',
  standalone: true,
  imports: [CdkDrag, CdkDragPlaceholder, MatChipsModule, MatTooltipModule, PriorityChipComponent],
  template: `
    <div
      class="task-card"
      cdkDrag
      [cdkDragData]="task()"
      [cdkDragDisabled]="isDragDisabled()"
    >
      <div class="task-card__header">
        <span class="task-card__title">{{ task().title }}</span>
        <tf-priority-chip [priority]="task().priority" />
      </div>

      @if (task().labels.length > 0) {
        <mat-chip-set class="task-card__labels">
          @for (label of task().labels; track label.id) {
            <mat-chip [style.background-color]="label.color">{{ label.name }}</mat-chip>
          }
        </mat-chip-set>
      }

      @if (task().assignee) {
        <div class="task-card__assignee" [matTooltip]="task().assignee!.name">
          <span class="task-card__avatar">{{ assigneeInitials() }}</span>
        </div>
      }

      <div class="cdk-drag-placeholder" *cdkDragPlaceholder></div>
    </div>
  `,
  styles: [
    `
      .task-card {
        background: white;
        border-radius: 6px;
        padding: 12px;
        margin-bottom: 8px;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
        cursor: grab;
        &:active {
          cursor: grabbing;
        }
      }
      .task-card__header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 8px;
      }
      .task-card__title {
        font-size: 0.875rem;
        font-weight: 500;
        flex: 1;
      }
      .task-card__labels {
        margin-top: 8px;
      }
      .task-card__assignee {
        margin-top: 8px;
        display: flex;
        justify-content: flex-end;
      }
      .task-card__avatar {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: #5c6bc0;
        color: white;
        font-size: 0.65rem;
        font-weight: 600;
      }
    `,
  ],
})
export class TaskCardComponent {
  readonly task = input.required<Task>();
  readonly taskClicked = output<Task>();

  protected isDragDisabled(): boolean {
    const status = this.task().status;
    return status === TaskStatus.DONE || status === TaskStatus.CANCELLED;
  }

  protected assigneeInitials(): string {
    const name = this.task().assignee?.name ?? '';
    return name
      .split(' ')
      .filter((part) => part.length > 0)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  }
}
