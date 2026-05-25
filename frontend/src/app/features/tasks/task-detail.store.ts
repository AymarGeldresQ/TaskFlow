import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CommentsApiClient } from '../../core/http/comments-api-client.service';
import { TasksApiClient } from '../../core/http/tasks-api-client.service';
import { BoardStore } from '../board/board.store';
import { TaskStatus } from '../../shared/models/task-status';
import { type Task, type Comment, type UpdateTaskRequest } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class TaskDetailStore {
  private readonly commentsApi = inject(CommentsApiClient);
  private readonly tasksApi = inject(TasksApiClient);
  private readonly boardStore = inject(BoardStore);

  readonly task = signal<Task | null>(null);
  readonly comments = signal<Comment[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  loadTask(taskId: string): void {
    const found = this.boardStore.tasks().find((t) => t.id === taskId) ?? null;
    this.task.set(found);
  }

  async loadComments(taskId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const list = await firstValueFrom(this.commentsApi.getComments(taskId));
      this.comments.set(list);
    } catch (e: unknown) {
      this.error.set(e instanceof Error ? e.message : 'Failed to load comments');
    } finally {
      this.loading.set(false);
    }
  }

  async addComment(taskId: string, body: string): Promise<void> {
    const comment = await firstValueFrom(this.commentsApi.addComment(taskId, { content: body }));
    this.comments.update((list) => [...list, comment]);
  }

  async editComment(commentId: string, body: string): Promise<void> {
    const updated = await firstValueFrom(
      this.commentsApi.editComment(commentId, { content: body }),
    );
    this.comments.update((list) => list.map((c) => (c.id === commentId ? updated : c)));
  }

  async deleteComment(commentId: string): Promise<void> {
    // Optimistic remove
    this.comments.update((list) => list.filter((c) => c.id !== commentId));
    await firstValueFrom(this.commentsApi.deleteComment(commentId));
  }

  async toggleLabel(taskId: string, labelId: string, attached: boolean): Promise<void> {
    const current = this.task();
    if (!current) return;

    // Optimistic update
    if (!attached) {
      this.task.set({
        ...current,
        labels: [...current.labels, { id: labelId, name: labelId, color: '#888', projectId: current.projectId }],
      });
    } else {
      this.task.set({
        ...current,
        labels: current.labels.filter((l) => l.id !== labelId),
      });
    }

    try {
      if (!attached) {
        await firstValueFrom(this.tasksApi.attachLabel(taskId, labelId));
      } else {
        await firstValueFrom(this.tasksApi.detachLabel(taskId, labelId));
      }
    } catch {
      // Rollback on error
      this.task.set(current);
      this.error.set('Failed to update label');
    }
  }

  async updateTask(taskId: string, req: UpdateTaskRequest): Promise<void> {
    const updated = await firstValueFrom(this.tasksApi.updateTask(taskId, req));
    this.task.set(updated);
    // Sync back to BoardStore
    this.boardStore.tasks.update((list) => list.map((t) => (t.id === taskId ? updated : t)));
  }

  async cancelTask(taskId: string): Promise<void> {
    await firstValueFrom(this.tasksApi.transitionStatus(taskId, TaskStatus.CANCELLED));
    this.task.set(null);
    // Remove from board
    this.boardStore.tasks.update((list) => list.filter((t) => t.id !== taskId));
  }
}
