import { Injectable, inject, signal, computed } from '@angular/core';
import { Subject, EMPTY } from 'rxjs';
import { groupBy, mergeMap, switchMap, catchError } from 'rxjs/operators';
import { firstValueFrom } from 'rxjs';
import { TasksApiClient } from '../../core/http/tasks-api-client.service';
import { TaskStatus } from '../../shared/models/task-status';
import { type Task, type CreateTaskRequest } from '../../shared/models';

export const BOARD_COLUMNS: TaskStatus[] = [
  TaskStatus.BACKLOG,
  TaskStatus.TODO,
  TaskStatus.IN_PROGRESS,
  TaskStatus.IN_REVIEW,
  TaskStatus.DONE,
];

export interface BoardFilters {
  statuses: TaskStatus[];
  assigneeIds: string[];
  labelIds: string[];
}

interface MoveEvent {
  taskId: string;
  from: TaskStatus;
  to: TaskStatus;
  generation: number;
}

@Injectable({ providedIn: 'root' })
export class BoardStore {
  private readonly api = inject(TasksApiClient);

  readonly tasks = signal<Task[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly filters = signal<BoardFilters>({ statuses: [], assigneeIds: [], labelIds: [] });

  // Per-task generation counters to guard against stale rollbacks
  private readonly generations = new Map<string, number>();

  readonly tasksByStatus = computed<Record<TaskStatus, Task[]>>(() => {
    const filtered = this.applyFilters(this.tasks());
    return BOARD_COLUMNS.reduce(
      (acc, status) => {
        acc[status] = filtered.filter((t) => t.status === status);
        return acc;
      },
      {} as Record<TaskStatus, Task[]>,
    );
  });

  private readonly move$ = new Subject<MoveEvent>();

  constructor() {
    this.move$
      .pipe(
        groupBy((e) => e.taskId),
        mergeMap((group$) =>
          group$.pipe(
            switchMap((event) => {
              const { taskId, from, to, generation } = event;
              // Optimistic update already applied in moveTask()
              return this.api.transitionStatus(taskId, to).pipe(
                catchError(() => {
                  // Only rollback if this generation is still the latest for this task
                  if (this.generations.get(taskId) === generation) {
                    this.tasks.update((current) =>
                      current.map((t) => (t.id === taskId ? { ...t, status: from } : t)),
                    );
                    this.error.set('Failed to update task status.');
                  }
                  return EMPTY;
                }),
              );
            }),
          ),
        ),
      )
      .subscribe();
  }

  async loadTasks(projectId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const page = await firstValueFrom(this.api.getTasks(projectId));
      this.tasks.set(page.content);
    } catch (e: unknown) {
      this.error.set(e instanceof Error ? e.message : 'Failed to load tasks');
    } finally {
      this.loading.set(false);
    }
  }

  moveTask(taskId: string, from: TaskStatus, to: TaskStatus): void {
    if (from === to) return;

    // Increment generation for this task — invalidates previous in-flight moves
    const generation = (this.generations.get(taskId) ?? 0) + 1;
    this.generations.set(taskId, generation);

    // Optimistic update: immediately reflect new status in the signal
    this.tasks.update((current) =>
      current.map((t) => (t.id === taskId ? { ...t, status: to } : t)),
    );

    this.move$.next({ taskId, from, to, generation });
  }

  async createTask(projectId: string, req: CreateTaskRequest): Promise<void> {
    try {
      const task = await firstValueFrom(this.api.createTask(projectId, req));
      this.tasks.update((current) => [...current, task]);
    } catch (e: unknown) {
      this.error.set(e instanceof Error ? e.message : 'Failed to create task');
    }
  }

  async deleteTask(taskId: string): Promise<void> {
    // Optimistic removal
    this.tasks.update((current) => current.filter((t) => t.id !== taskId));
    try {
      await firstValueFrom(this.api.deleteTask(taskId));
    } catch (e: unknown) {
      this.error.set(e instanceof Error ? e.message : 'Failed to delete task');
    }
  }

  setFilters(filters: BoardFilters): void {
    this.filters.set(filters);
  }

  private applyFilters(tasks: Task[]): Task[] {
    // Never show CANCELLED tasks on the board
    let result = tasks.filter((t) => t.status !== TaskStatus.CANCELLED);

    const { statuses, assigneeIds, labelIds } = this.filters();

    if (statuses.length > 0) {
      result = result.filter((t) => statuses.includes(t.status));
    }

    if (assigneeIds.length > 0) {
      result = result.filter((t) => t.assignee !== null && assigneeIds.includes(t.assignee.id));
    }

    if (labelIds.length > 0) {
      result = result.filter((t) => t.labels.some((l) => labelIds.includes(l.id)));
    }

    return result;
  }
}
