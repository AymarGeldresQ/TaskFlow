import { type TaskStatus } from './task-status';
import { type Label } from './label.model';
import { type User } from './auth.model';

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface Task {
  id: string;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  projectId: string;
  assignee: User | null;
  labels: Label[];
  dueDate: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  priority: TaskPriority;
  projectId: string;
  assigneeId?: string;
  dueDate?: string;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string;
  priority?: TaskPriority;
  assigneeId?: string | null;
  dueDate?: string | null;
}

export interface TransitionTaskRequest {
  targetStatus: TaskStatus;
}

export interface AssignTaskRequest {
  assigneeId: string | null;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}
