import { Component, input, output } from '@angular/core';
import { type CdkDrag, CdkDragDrop, CdkDropList, transferArrayItem } from '@angular/cdk/drag-drop';
import { TaskCardComponent } from '../task-card/task-card.component';
import { type Task } from '../../../../shared/models';
import { TaskStatus, canTransition } from '../../../../shared/models/task-status';

@Component({
  selector: 'tf-board-column',
  standalone: true,
  imports: [CdkDropList, TaskCardComponent],
  template: `
    <div class="board-column">
      <div class="board-column__header">
        <span class="board-column__title">{{ status() }}</span>
        <span class="board-column__count">{{ tasks().length }}</span>
      </div>

      <div
        class="board-column__list"
        cdkDropList
        [id]="status()"
        [cdkDropListData]="tasks()"
        [cdkDropListEnterPredicate]="canDrop"
        (cdkDropListDropped)="onDrop($event)"
      >
        @for (task of tasks(); track task.id) {
          <tf-task-card [task]="task" (taskClicked)="taskClicked.emit($event)" />
        }
      </div>
    </div>
  `,
  styles: [
    `
      .board-column {
        display: flex;
        flex-direction: column;
        min-width: 240px;
        max-width: 280px;
        background: #f5f5f5;
        border-radius: 8px;
        padding: 12px;
      }
      .board-column__header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        padding-bottom: 8px;
        border-bottom: 2px solid #e0e0e0;
      }
      .board-column__title {
        font-size: 0.75rem;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: #616161;
      }
      .board-column__count {
        font-size: 0.75rem;
        font-weight: 600;
        background: #e0e0e0;
        border-radius: 10px;
        padding: 2px 8px;
        color: #424242;
      }
      .board-column__list {
        flex: 1;
        min-height: 60px;
      }
    `,
  ],
})
export class BoardColumnComponent {
  readonly status = input.required<TaskStatus>();
  readonly tasks = input<Task[]>([]);
  readonly taskMoved = output<{ taskId: string; from: TaskStatus; to: TaskStatus }>();
  readonly taskClicked = output<Task>();

  /** CDK predicate: block drops that would violate the state machine */
  readonly canDrop = (drag: CdkDrag<Task>): boolean =>
    canTransition(drag.data.status, this.status());

  protected onDrop(event: CdkDragDrop<Task[]>): void {
    const from = event.previousContainer.id as TaskStatus;
    const to = event.container.id as TaskStatus;

    if (from === to) return;

    const task = event.previousContainer.data[event.previousIndex];
    if (!canTransition(from, to)) return;

    // Move the item in the local arrays so CDK doesn't glitch visually
    // (the store will reconcile the real state via optimistic update)
    transferArrayItem(
      event.previousContainer.data,
      event.container.data,
      event.previousIndex,
      event.currentIndex,
    );

    this.taskMoved.emit({ taskId: task.id, from, to });
  }
}
