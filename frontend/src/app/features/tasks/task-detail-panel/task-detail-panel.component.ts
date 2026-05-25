import { Component, inject, input, output, signal, effect } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { AuthStore } from '../../../core/auth/auth.store';
import { LabelsStore } from '../../labels/labels.store';
import { TaskDetailStore } from '../task-detail.store';
import { TaskEditFormComponent } from '../components/task-edit-form/task-edit-form.component';
import { CommentListComponent } from '../components/comment-list/comment-list.component';
import { CommentFormComponent } from '../components/comment-form/comment-form.component';
import { LabelSelectorComponent } from '../components/label-selector/label-selector.component';
import { UserAvatarComponent } from '../../../shared/ui/user-avatar/user-avatar.component';
import { PriorityChipComponent } from '../../../shared/ui/priority-chip/priority-chip.component';
import { type UpdateTaskRequest } from '../../../shared/models';
import { TaskStatus } from '../../../shared/models/task-status';

@Component({
  selector: 'tf-task-detail-panel',
  standalone: true,
  imports: [
    SlicePipe,
    MatButtonModule,
    MatDialogModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    TaskEditFormComponent,
    CommentListComponent,
    CommentFormComponent,
    LabelSelectorComponent,
    UserAvatarComponent,
    PriorityChipComponent,
  ],
  template: `
    @if (taskId()) {
      <div class="detail-panel__backdrop" (click)="close()"></div>
      <aside class="detail-panel" role="complementary" aria-label="Task detail">
        <div class="detail-panel__header">
          <h2 class="detail-panel__title">Task Detail</h2>
          <button mat-icon-button aria-label="Close panel" (click)="close()">
            <mat-icon>close</mat-icon>
          </button>
        </div>

        @if (store.loading()) {
          <div class="detail-panel__loading">
            <mat-spinner diameter="32" />
          </div>
        } @else if (store.task(); as task) {
          <div class="detail-panel__body">

            @if (editing()) {
              <tf-task-edit-form
                [task]="task"
                (saved)="onSave($event)"
                (cancelled)="editing.set(false)"
              />
            } @else {
              <div class="detail-panel__view">
                <div class="detail-panel__field">
                  <span class="detail-panel__label">Title</span>
                  <p class="detail-panel__value">{{ task.title }}</p>
                </div>

                @if (task.description) {
                  <div class="detail-panel__field">
                    <span class="detail-panel__label">Description</span>
                    <p class="detail-panel__value">{{ task.description }}</p>
                  </div>
                }

                <div class="detail-panel__row">
                  <div class="detail-panel__field">
                    <span class="detail-panel__label">Priority</span>
                    <tf-priority-chip [priority]="task.priority" />
                  </div>

                  @if (task.dueDate) {
                    <div class="detail-panel__field">
                      <span class="detail-panel__label">Due</span>
                      <span class="detail-panel__value">{{ task.dueDate | slice: 0 : 10 }}</span>
                    </div>
                  }
                </div>

                @if (task.assignee) {
                  <div class="detail-panel__field">
                    <span class="detail-panel__label">Assignee</span>
                    <tf-user-avatar [user]="task.assignee" />
                  </div>
                }

                <button mat-button (click)="editing.set(true)">Edit</button>
              </div>
            }

            <mat-divider />

            <div class="detail-panel__section">
              <h3 class="detail-panel__section-title">Labels</h3>
              <tf-label-selector
                [availableLabels]="labelsStore.labels()"
                [attachedLabelIds]="task.labels.map(l => l.id)"
                (toggle)="onToggleLabel(task.id, $event.labelId, $event.attached)"
              />
            </div>

            <mat-divider />

            @if (canCancel(task.status)) {
              <button mat-button color="warn" (click)="onCancelTask(task.id)">
                Cancel task
              </button>
            }

            <mat-divider />

            <div class="detail-panel__section">
              <h3 class="detail-panel__section-title">Comments</h3>
              <tf-comment-list
                [comments]="store.comments()"
                [currentUserId]="currentUserId()"
                (editComment)="onEditComment($event.id, $event.body)"
                (deleteComment)="onDeleteComment($event)"
              />
              <tf-comment-form (submitted)="onAddComment(task.id, $event)" />
            </div>
          </div>
        }
      </aside>
    }
  `,
  styles: [
    `
      .detail-panel__backdrop {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.3);
        z-index: 900;
      }
      .detail-panel {
        position: fixed;
        top: 0;
        right: 0;
        bottom: 0;
        width: 420px;
        max-width: 100vw;
        background: white;
        z-index: 901;
        display: flex;
        flex-direction: column;
        box-shadow: -4px 0 24px rgba(0, 0, 0, 0.15);
        overflow-y: auto;
      }
      .detail-panel__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 16px;
        border-bottom: 1px solid #e0e0e0;
        position: sticky;
        top: 0;
        background: white;
        z-index: 1;
      }
      .detail-panel__title {
        font-size: 1.1rem;
        font-weight: 600;
        margin: 0;
      }
      .detail-panel__loading {
        display: flex;
        justify-content: center;
        padding: 32px;
      }
      .detail-panel__body {
        padding: 16px;
        display: flex;
        flex-direction: column;
        gap: 16px;
      }
      .detail-panel__field {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }
      .detail-panel__row {
        display: flex;
        gap: 16px;
      }
      .detail-panel__label {
        font-size: 0.75rem;
        font-weight: 600;
        color: #757575;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      .detail-panel__value {
        margin: 0;
        font-size: 0.9rem;
      }
      .detail-panel__section {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .detail-panel__section-title {
        font-size: 0.875rem;
        font-weight: 600;
        margin: 0;
        color: #424242;
      }
    `,
  ],
})
export class TaskDetailPanelComponent {
  readonly taskId = input<string | null>(null);
  readonly panelClosed = output();

  protected readonly store = inject(TaskDetailStore);
  protected readonly labelsStore = inject(LabelsStore);
  private readonly auth = inject(AuthStore);
  private readonly dialog = inject(MatDialog);

  protected readonly editing = signal(false);

  constructor() {
    effect(() => {
      const taskId = this.taskId();
      if (taskId) {
        this.editing.set(false);
        this.store.loadTask(taskId);
        void this.store.loadComments(taskId);
      }
    });
  }

  protected currentUserId(): string {
    return this.auth.currentUser()?.id ?? '';
  }

  protected close(): void {
    this.panelClosed.emit();
  }

  protected canCancel(status: TaskStatus): boolean {
    return status !== TaskStatus.DONE && status !== TaskStatus.CANCELLED;
  }

  protected onSave(req: UpdateTaskRequest): void {
    const task = this.store.task();
    if (!task) return;
    void this.store.updateTask(task.id, req);
    this.editing.set(false);
  }

  protected onToggleLabel(taskId: string, labelId: string, attached: boolean): void {
    void this.store.toggleLabel(taskId, labelId, attached);
  }

  protected onCancelTask(taskId: string): void {
    // Confirmation via MatDialog API (simple confirm pattern without a separate component)
    const confirmed = window.confirm('Cancel this task? This action cannot be undone.');
    if (confirmed) {
      void this.store.cancelTask(taskId);
      this.close();
    }
  }

  protected onAddComment(taskId: string, body: string): void {
    void this.store.addComment(taskId, body);
  }

  protected onEditComment(commentId: string, body: string): void {
    void this.store.editComment(commentId, body);
  }

  protected onDeleteComment(commentId: string): void {
    void this.store.deleteComment(commentId);
  }
}
