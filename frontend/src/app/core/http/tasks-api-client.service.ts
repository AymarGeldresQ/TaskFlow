import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { type Observable } from 'rxjs';
import {
  type Task,
  type CreateTaskRequest,
  type UpdateTaskRequest,
  type PageResponse,
  TaskStatus,
} from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class TasksApiClient {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  getTasks(
    projectId: string,
    params?: { status?: TaskStatus; page?: number; size?: number },
  ): Observable<PageResponse<Task>> {
    const p = new HttpParams()
      .set('page', String(params?.page ?? 0))
      .set('size', String(params?.size ?? 100));
    return this.http.get<PageResponse<Task>>(`${this.base}/projects/${projectId}/tasks`, {
      params: p,
    });
  }

  createTask(projectId: string, req: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(`${this.base}/projects/${projectId}/tasks`, req);
  }

  updateTask(taskId: string, req: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.base}/tasks/${taskId}`, req);
  }

  transitionStatus(taskId: string, targetStatus: TaskStatus): Observable<Task> {
    return this.http.patch<Task>(`${this.base}/tasks/${taskId}/status`, { targetStatus });
  }

  assignTask(taskId: string, assigneeId: string | null): Observable<Task> {
    return this.http.patch<Task>(`${this.base}/tasks/${taskId}/assignee`, { assigneeId });
  }

  deleteTask(taskId: string): Observable<unknown> {
    return this.http.delete(`${this.base}/tasks/${taskId}`);
  }

  attachLabel(taskId: string, labelId: string): Observable<unknown> {
    return this.http.post(`${this.base}/tasks/${taskId}/labels/${labelId}`, {});
  }

  detachLabel(taskId: string, labelId: string): Observable<unknown> {
    return this.http.delete(`${this.base}/tasks/${taskId}/labels/${labelId}`);
  }
}
