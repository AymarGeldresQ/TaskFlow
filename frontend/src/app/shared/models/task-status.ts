export enum TaskStatus {
  BACKLOG = 'BACKLOG',
  TODO = 'TODO',
  IN_PROGRESS = 'IN_PROGRESS',
  IN_REVIEW = 'IN_REVIEW',
  DONE = 'DONE',
  CANCELLED = 'CANCELLED',
}

export const TASK_STATUS_TRANSITIONS: Record<TaskStatus, TaskStatus[]> = {
  [TaskStatus.BACKLOG]: [TaskStatus.TODO, TaskStatus.CANCELLED],
  [TaskStatus.TODO]: [TaskStatus.IN_PROGRESS, TaskStatus.BACKLOG, TaskStatus.CANCELLED],
  [TaskStatus.IN_PROGRESS]: [TaskStatus.IN_REVIEW, TaskStatus.TODO, TaskStatus.CANCELLED],
  [TaskStatus.IN_REVIEW]: [TaskStatus.DONE, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED],
  [TaskStatus.DONE]: [],
  [TaskStatus.CANCELLED]: [],
};

export function canTransition(from: TaskStatus, to: TaskStatus): boolean {
  return TASK_STATUS_TRANSITIONS[from].includes(to);
}
