import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TaskDetailStore } from './task-detail.store';
import { CommentsApiClient } from '../../core/http/comments-api-client.service';
import { TasksApiClient } from '../../core/http/tasks-api-client.service';
import { BoardStore } from '../board/board.store';
import { TaskStatus } from '../../shared/models/task-status';
import { type Task, type Comment } from '../../shared/models';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

const makeTask = (id: string, status: TaskStatus = TaskStatus.TODO): Task => ({
  id,
  title: `Task ${id}`,
  description: null,
  status,
  priority: 'MEDIUM',
  projectId: 'proj-1',
  assignee: null,
  labels: [],
  dueDate: null,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
});

const makeComment = (id: string, authorId = 'user-1'): Comment => ({
  id,
  taskId: 'task-1',
  authorId,
  authorName: 'Alice',
  content: `Comment ${id}`,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
});

describe('TaskDetailStore', () => {
  let store: TaskDetailStore;
  let commentsApiMock: Partial<CommentsApiClient>;
  let tasksApiMock: Partial<TasksApiClient>;
  let boardStoreMock: Partial<BoardStore>;

  beforeEach(() => {
    commentsApiMock = {
      getComments: vi.fn().mockReturnValue(of([makeComment('c1'), makeComment('c2')])),
      addComment: vi.fn().mockReturnValue(of(makeComment('c3'))),
      editComment: vi.fn().mockReturnValue(of(makeComment('c1'))),
      deleteComment: vi.fn().mockReturnValue(of(undefined)),
    };

    tasksApiMock = {
      updateTask: vi.fn().mockReturnValue(of(makeTask('task-1'))),
      transitionStatus: vi.fn().mockReturnValue(of(makeTask('task-1', TaskStatus.CANCELLED))),
      attachLabel: vi.fn().mockReturnValue(of(undefined)),
      detachLabel: vi.fn().mockReturnValue(of(undefined)),
    };

    boardStoreMock = {
      tasks: { set: vi.fn(), update: vi.fn() } as unknown as BoardStore['tasks'],
    };

    // Provide a real signal for tasks via a simple value
    const tasksArr = [makeTask('task-1'), makeTask('task-2')];
    Object.assign(boardStoreMock, {
      tasks: Object.assign(() => tasksArr, { set: vi.fn(), update: vi.fn() }),
    });

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: CommentsApiClient, useValue: commentsApiMock },
        { provide: TasksApiClient, useValue: tasksApiMock },
        { provide: BoardStore, useValue: boardStoreMock },
      ],
    });

    store = TestBed.inject(TaskDetailStore);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  // Test: loadTask sets task from BoardStore
  it('loadTask sets task signal from BoardStore tasks', () => {
    store.loadTask('task-1');
    expect(store.task()).not.toBeNull();
    expect(store.task()?.id).toBe('task-1');
  });

  it('loadTask sets task to null if task not found in board', () => {
    store.loadTask('missing-task');
    expect(store.task()).toBeNull();
  });

  // Test: loadComments sets comments signal
  it('loadComments sets comments signal from API response', async () => {
    (commentsApiMock.getComments as ReturnType<typeof vi.fn>).mockReturnValue(
      of([makeComment('c1'), makeComment('c2')]),
    );
    await store.loadComments('task-1');
    expect(store.comments()).toHaveLength(2);
    expect(store.comments()[0].id).toBe('c1');
  });

  it('loadComments sets loading true then false', async () => {
    const states: boolean[] = [];
    const origSet = store.loading.set.bind(store.loading);
    vi.spyOn(store.loading, 'set').mockImplementation((v) => {
      states.push(v);
      origSet(v);
    });
    await store.loadComments('task-1');
    expect(states).toContain(true);
    expect(store.loading()).toBe(false);
  });

  // Test: addComment optimistic add
  it('addComment optimistically appends new comment to the list', async () => {
    await store.loadComments('task-1');
    const before = store.comments().length;
    await store.addComment('task-1', 'New comment');
    expect(store.comments().length).toBe(before + 1);
    expect(store.comments().at(-1)?.id).toBe('c3');
  });

  // Test: deleteComment optimistic remove
  it('deleteComment optimistically removes the comment from the list', async () => {
    await store.loadComments('task-1');
    expect(store.comments()).toHaveLength(2);
    await store.deleteComment('c1');
    expect(store.comments().find((c) => c.id === 'c1')).toBeUndefined();
  });

  // Test: toggleLabel optimistic + rollback on error
  it('toggleLabel optimistically adds label to task', () => {
    store.loadTask('task-1');
    const task = store.task()!;
    expect(task.labels).toHaveLength(0);
    void store.toggleLabel('task-1', 'label-1', false);
    expect(store.task()?.labels.some((l) => l.id === 'label-1')).toBe(true);
  });

  it('toggleLabel rolls back label on API error', async () => {
    (tasksApiMock.attachLabel as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('fail')),
    );
    store.loadTask('task-1');
    await store.toggleLabel('task-1', 'label-1', false);
    // After error, the optimistic add should be reverted — label no longer in task
    expect(store.task()?.labels.some((l) => l.id === 'label-1')).toBe(false);
  });

  // Test: cancelTask removes task from BoardStore
  it('cancelTask removes the task from BoardStore after PATCH', async () => {
    store.loadTask('task-1');
    await store.cancelTask('task-1');
    expect(tasksApiMock.transitionStatus).toHaveBeenCalledWith('task-1', TaskStatus.CANCELLED);
    expect(boardStoreMock.tasks!.update).toHaveBeenCalled();
  });
});
