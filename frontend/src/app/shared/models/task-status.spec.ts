import { describe, it, expect } from 'vitest';
import { TaskStatus, TASK_STATUS_TRANSITIONS, canTransition } from './task-status';

describe('TASK_STATUS_TRANSITIONS', () => {
  it('BACKLOG can transition to TODO and CANCELLED only', () => {
    expect(TASK_STATUS_TRANSITIONS[TaskStatus.BACKLOG]).toEqual([TaskStatus.TODO, TaskStatus.CANCELLED]);
  });

  it('TODO can transition to IN_PROGRESS, BACKLOG, and CANCELLED only', () => {
    expect(TASK_STATUS_TRANSITIONS[TaskStatus.TODO]).toEqual([
      TaskStatus.IN_PROGRESS,
      TaskStatus.BACKLOG,
      TaskStatus.CANCELLED,
    ]);
  });

  it('IN_PROGRESS can transition to IN_REVIEW, TODO, and CANCELLED only', () => {
    expect(TASK_STATUS_TRANSITIONS[TaskStatus.IN_PROGRESS]).toEqual([
      TaskStatus.IN_REVIEW,
      TaskStatus.TODO,
      TaskStatus.CANCELLED,
    ]);
  });

  it('IN_REVIEW can transition to DONE, IN_PROGRESS, and CANCELLED only', () => {
    expect(TASK_STATUS_TRANSITIONS[TaskStatus.IN_REVIEW]).toEqual([
      TaskStatus.DONE,
      TaskStatus.IN_PROGRESS,
      TaskStatus.CANCELLED,
    ]);
  });

  it('DONE has no valid transitions (terminal)', () => {
    expect(TASK_STATUS_TRANSITIONS[TaskStatus.DONE]).toEqual([]);
  });

  it('CANCELLED has no valid transitions (terminal)', () => {
    expect(TASK_STATUS_TRANSITIONS[TaskStatus.CANCELLED]).toEqual([]);
  });

  it('covers all six statuses', () => {
    const keys = Object.keys(TASK_STATUS_TRANSITIONS);
    expect(keys).toHaveLength(6);
    expect(keys).toContain(TaskStatus.BACKLOG);
    expect(keys).toContain(TaskStatus.TODO);
    expect(keys).toContain(TaskStatus.IN_PROGRESS);
    expect(keys).toContain(TaskStatus.IN_REVIEW);
    expect(keys).toContain(TaskStatus.DONE);
    expect(keys).toContain(TaskStatus.CANCELLED);
  });
});

describe('canTransition', () => {
  it('returns true for valid transitions', () => {
    expect(canTransition(TaskStatus.TODO, TaskStatus.IN_PROGRESS)).toBe(true);
    expect(canTransition(TaskStatus.TODO, TaskStatus.BACKLOG)).toBe(true);
    expect(canTransition(TaskStatus.IN_REVIEW, TaskStatus.DONE)).toBe(true);
  });

  it('returns false for invalid transitions', () => {
    expect(canTransition(TaskStatus.IN_REVIEW, TaskStatus.BACKLOG)).toBe(false);
    expect(canTransition(TaskStatus.DONE, TaskStatus.TODO)).toBe(false);
    expect(canTransition(TaskStatus.CANCELLED, TaskStatus.TODO)).toBe(false);
    expect(canTransition(TaskStatus.IN_PROGRESS, TaskStatus.BACKLOG)).toBe(false);
  });

  it('returns false for same-status transition', () => {
    expect(canTransition(TaskStatus.TODO, TaskStatus.TODO)).toBe(false);
  });
});
