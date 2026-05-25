import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { of, throwError, Subject } from 'rxjs';
import { BoardStore } from './board.store';
import { TasksApiClient } from '../../core/http/tasks-api-client.service';
import { TaskStatus } from '../../shared/models/task-status';
import { type Task, type PageResponse } from '../../shared/models';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

const makeTask = (id: string, status: TaskStatus): Task => ({
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

const mockPage = (tasks: Task[]): PageResponse<Task> => ({
  content: tasks,
  totalElements: tasks.length,
  totalPages: 1,
  page: 0,
  size: 100,
});

/** Flush microtask queue — allows RxJS synchronous observables to complete */
const flushMicrotasks = (): Promise<void> => Promise.resolve();

describe('BoardStore', () => {
  let store: BoardStore;
  let apiMock: Partial<TasksApiClient>;

  beforeEach(() => {
    apiMock = {
      getTasks: vi.fn().mockReturnValue(of(mockPage([]))),
      transitionStatus: vi.fn().mockReturnValue(of(makeTask('task-1', TaskStatus.IN_PROGRESS))),
      createTask: vi.fn().mockReturnValue(of(makeTask('new-task', TaskStatus.BACKLOG))),
      deleteTask: vi.fn().mockReturnValue(of(undefined)),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TasksApiClient, useValue: apiMock },
      ],
    });

    store = TestBed.inject(BoardStore);
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  // Test 1: loadTasks sets tasks signal
  it('loadTasks populates tasks signal from API response', async () => {
    const tasks = [makeTask('t1', TaskStatus.TODO), makeTask('t2', TaskStatus.BACKLOG)];
    (apiMock.getTasks as ReturnType<typeof vi.fn>).mockReturnValue(of(mockPage(tasks)));

    await store.loadTasks('proj-1');

    expect(store.tasks()).toEqual(tasks);
  });

  // Test 2: loadTasks sets loading
  it('loadTasks sets loading true then false', async () => {
    const states: boolean[] = [];
    const origSet = store.loading.set.bind(store.loading);
    vi.spyOn(store.loading, 'set').mockImplementation((v) => {
      states.push(v);
      origSet(v);
    });
    await store.loadTasks('proj-1');
    expect(states).toContain(true);
    expect(store.loading()).toBe(false);
  });

  // Test 3: tasksByStatus computed groups tasks by status
  it('tasksByStatus groups tasks by status column', async () => {
    const tasks = [
      makeTask('t1', TaskStatus.TODO),
      makeTask('t2', TaskStatus.TODO),
      makeTask('t3', TaskStatus.IN_PROGRESS),
      makeTask('t4', TaskStatus.BACKLOG),
    ];
    (apiMock.getTasks as ReturnType<typeof vi.fn>).mockReturnValue(of(mockPage(tasks)));
    await store.loadTasks('proj-1');

    const byStatus = store.tasksByStatus();
    expect(byStatus[TaskStatus.TODO]).toHaveLength(2);
    expect(byStatus[TaskStatus.IN_PROGRESS]).toHaveLength(1);
    expect(byStatus[TaskStatus.BACKLOG]).toHaveLength(1);
    expect(byStatus[TaskStatus.DONE]).toHaveLength(0);
  });

  // Test 4: tasksByStatus excludes CANCELLED tasks
  it('tasksByStatus excludes CANCELLED tasks from all board columns', async () => {
    const tasks = [
      makeTask('t1', TaskStatus.TODO),
      makeTask('t2', TaskStatus.CANCELLED),
    ];
    (apiMock.getTasks as ReturnType<typeof vi.fn>).mockReturnValue(of(mockPage(tasks)));
    await store.loadTasks('proj-1');

    const byStatus = store.tasksByStatus();
    expect(byStatus[TaskStatus.TODO]).toHaveLength(1);
    const allBoardTasks = Object.values(byStatus).flat();
    expect(allBoardTasks.find((t) => t.status === TaskStatus.CANCELLED)).toBeUndefined();
  });

  // Test 5: moveTask optimistic success — task moves to new column
  it('moveTask optimistically moves task to target column before API responds', async () => {
    const task = makeTask('task-1', TaskStatus.TODO);
    store.tasks.set([task]);

    // API returns synchronously (of(...)) — optimistic update happens in moveTask call itself
    (apiMock.transitionStatus as ReturnType<typeof vi.fn>).mockReturnValue(
      of(makeTask('task-1', TaskStatus.IN_PROGRESS)),
    );

    store.moveTask('task-1', TaskStatus.TODO, TaskStatus.IN_PROGRESS);

    // Optimistic update must be immediate — signal reflects new status before any await
    expect(store.tasks().find((t) => t.id === 'task-1')?.status).toBe(TaskStatus.IN_PROGRESS);

    // After microtasks: task remains in IN_PROGRESS
    await flushMicrotasks();
    expect(store.tasks().find((t) => t.id === 'task-1')?.status).toBe(TaskStatus.IN_PROGRESS);
  });

  // Test 6: moveTask rollback — API fails → task reverts, error signal set
  it('moveTask rolls back to original status when API fails and sets error signal', async () => {
    const task = makeTask('task-1', TaskStatus.TODO);
    store.tasks.set([task]);

    // throwError is synchronous — catchError runs immediately within move$.next()
    (apiMock.transitionStatus as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('HTTP 422')),
    );

    store.moveTask('task-1', TaskStatus.TODO, TaskStatus.IN_PROGRESS);
    await flushMicrotasks();

    // After synchronous rollback: task is back to original TODO status
    expect(store.tasks().find((t) => t.id === 'task-1')?.status).toBe(TaskStatus.TODO);
    // Error signal must be set to inform the user
    expect(store.error()).toBeTruthy();
    expect(store.error()).toContain('Failed');
  });

  // Test 7: generation guard — second move supersedes first → stale rollback does NOT fire
  it('generation guard prevents stale rollback from a superseded first move', async () => {
    const task = makeTask('task-1', TaskStatus.TODO);
    store.tasks.set([task]);

    const firstMoveSubject = new Subject<Task>();

    let callCount = 0;
    (apiMock.transitionStatus as ReturnType<typeof vi.fn>).mockImplementation(() => {
      callCount++;
      if (callCount === 1) {
        return firstMoveSubject.asObservable();
      }
      // Second call resolves immediately
      return of(makeTask('task-1', TaskStatus.IN_REVIEW));
    });

    // First move: TODO → IN_PROGRESS (deferred — subject not completed yet)
    store.moveTask('task-1', TaskStatus.TODO, TaskStatus.IN_PROGRESS);
    await flushMicrotasks();

    // Second move: IN_PROGRESS → IN_REVIEW (supersedes first via switchMap)
    store.moveTask('task-1', TaskStatus.IN_PROGRESS, TaskStatus.IN_REVIEW);
    await flushMicrotasks();

    // Now the second move's API has resolved — task is at IN_REVIEW
    expect(store.tasks().find((t) => t.id === 'task-1')?.status).toBe(TaskStatus.IN_REVIEW);

    // Fail the first move — generation guard should block rollback since generation was superseded
    firstMoveSubject.error(new Error('HTTP 422'));
    await flushMicrotasks();

    // Task must remain at IN_REVIEW — stale rollback was blocked
    expect(store.tasks().find((t) => t.id === 'task-1')?.status).toBe(TaskStatus.IN_REVIEW);
    // No error signal set from the stale first move
    expect(store.error()).toBeNull();
  });

  // Test 8: setFilters with status filter limits tasksByStatus
  it('setFilters with status filter limits tasksByStatus to matching tasks', async () => {
    const tasks = [
      makeTask('t1', TaskStatus.TODO),
      makeTask('t2', TaskStatus.IN_PROGRESS),
      makeTask('t3', TaskStatus.BACKLOG),
    ];
    (apiMock.getTasks as ReturnType<typeof vi.fn>).mockReturnValue(of(mockPage(tasks)));
    await store.loadTasks('proj-1');

    store.setFilters({ statuses: [TaskStatus.TODO], assigneeIds: [], labelIds: [] });

    const byStatus = store.tasksByStatus();
    expect(byStatus[TaskStatus.TODO]).toHaveLength(1);
    expect(byStatus[TaskStatus.IN_PROGRESS]).toHaveLength(0);
    expect(byStatus[TaskStatus.BACKLOG]).toHaveLength(0);
  });

  // Test 9: setFilters — clearing filters shows all tasks
  it('setFilters with empty statuses shows all non-cancelled tasks', async () => {
    const tasks = [
      makeTask('t1', TaskStatus.TODO),
      makeTask('t2', TaskStatus.IN_PROGRESS),
      makeTask('t3', TaskStatus.BACKLOG),
    ];
    (apiMock.getTasks as ReturnType<typeof vi.fn>).mockReturnValue(of(mockPage(tasks)));
    await store.loadTasks('proj-1');

    store.setFilters({ statuses: [], assigneeIds: [], labelIds: [] });

    const allBoardTasks = Object.values(store.tasksByStatus()).flat();
    expect(allBoardTasks).toHaveLength(3);
  });
});
