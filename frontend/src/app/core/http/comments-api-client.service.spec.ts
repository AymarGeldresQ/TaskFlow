import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CommentsApiClient } from './comments-api-client.service';
import { type Comment } from '../../shared/models';

describe('CommentsApiClient', () => {
  let service: CommentsApiClient;
  let httpMock: HttpTestingController;

  const mockComment: Comment = {
    id: 'comment-1',
    taskId: 'task-1',
    authorId: 'user-1',
    authorName: 'Alice',
    content: 'First comment',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CommentsApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getComments sends GET to /api/v1/tasks/{taskId}/comments', () => {
    service.getComments('task-1').subscribe();
    const req = httpMock.expectOne('/api/v1/tasks/task-1/comments');
    expect(req.request.method).toBe('GET');
    req.flush([mockComment]);
  });

  it('addComment sends POST to /api/v1/tasks/{taskId}/comments with body', () => {
    service.addComment('task-1', { content: 'Hello' }).subscribe();
    const req = httpMock.expectOne('/api/v1/tasks/task-1/comments');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ content: 'Hello' });
    req.flush(mockComment);
  });

  it('editComment sends PUT to /api/v1/comments/{commentId} with body', () => {
    service.editComment('comment-1', { content: 'Updated' }).subscribe();
    const req = httpMock.expectOne('/api/v1/comments/comment-1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ content: 'Updated' });
    req.flush({ ...mockComment, content: 'Updated' });
  });

  it('deleteComment sends DELETE to /api/v1/comments/{commentId}', () => {
    service.deleteComment('comment-1').subscribe();
    const req = httpMock.expectOne('/api/v1/comments/comment-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
