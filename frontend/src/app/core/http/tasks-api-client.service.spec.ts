import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TasksApiClient } from './tasks-api-client.service';
import { TaskStatus } from '../../shared/models/task-status';
import { type Task, type CreateTaskRequest, type UpdateTaskRequest, type PageResponse } from '../../shared/models';

describe('TasksApiClient', () => {
  let service: TasksApiClient;
  let httpMock: HttpTestingController;

  const mockTask: Task = {
    id: 'task-1',
    title: 'Fix bug',
    description: null,
    status: TaskStatus.TODO,
    priority: 'HIGH',
    projectId: 'proj-1',
    assignee: null,
    labels: [],
    dueDate: null,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  };

  const mockPage: PageResponse<Task> = {
    content: [mockTask],
    totalElements: 1,
    totalPages: 1,
    page: 0,
    size: 100,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TasksApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getTasks sends GET to /api/v1/projects/{projectId}/tasks', () => {
    service.getTasks('proj-1').subscribe();
    const req = httpMock.expectOne('/api/v1/projects/proj-1/tasks?page=0&size=100');
    expect(req.request.method).toBe('GET');
    req.flush(mockPage);
  });

  it('createTask sends POST to /api/v1/projects/{projectId}/tasks', () => {
    const createReq: CreateTaskRequest = {
      title: 'New task',
      priority: 'MEDIUM',
      projectId: 'proj-1',
    };
    service.createTask('proj-1', createReq).subscribe();
    const req = httpMock.expectOne('/api/v1/projects/proj-1/tasks');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(createReq);
    req.flush(mockTask);
  });

  it('updateTask sends PUT to /api/v1/tasks/{taskId}', () => {
    const updateReq: UpdateTaskRequest = { title: 'Updated' };
    service.updateTask('task-1', updateReq).subscribe();
    const req = httpMock.expectOne('/api/v1/tasks/task-1');
    expect(req.request.method).toBe('PUT');
    req.flush(mockTask);
  });

  it('transitionStatus sends PATCH to /api/v1/tasks/{taskId}/status', () => {
    service.transitionStatus('task-1', TaskStatus.IN_PROGRESS).subscribe();
    const req = httpMock.expectOne('/api/v1/tasks/task-1/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ targetStatus: TaskStatus.IN_PROGRESS });
    req.flush(mockTask);
  });

  it('assignTask sends PATCH to /api/v1/tasks/{taskId}/assignee', () => {
    service.assignTask('task-1', 'user-1').subscribe();
    const req = httpMock.expectOne('/api/v1/tasks/task-1/assignee');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ assigneeId: 'user-1' });
    req.flush(mockTask);
  });

  it('deleteTask sends DELETE to /api/v1/tasks/{taskId}', () => {
    service.deleteTask('task-1').subscribe();
    const req = httpMock.expectOne('/api/v1/tasks/task-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('attachLabel sends POST to /api/v1/tasks/{taskId}/labels/{labelId}', () => {
    service.attachLabel('task-1', 'label-1').subscribe();
    const req = httpMock.expectOne('/api/v1/tasks/task-1/labels/label-1');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });

  it('detachLabel sends DELETE to /api/v1/tasks/{taskId}/labels/{labelId}', () => {
    service.detachLabel('task-1', 'label-1').subscribe();
    const req = httpMock.expectOne('/api/v1/tasks/task-1/labels/label-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
