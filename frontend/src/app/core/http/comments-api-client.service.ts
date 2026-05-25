import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable } from 'rxjs';
import { type Comment, type AddCommentRequest, type EditCommentRequest } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class CommentsApiClient {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';

  getComments(taskId: string): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.base}/tasks/${taskId}/comments`);
  }

  addComment(taskId: string, req: AddCommentRequest): Observable<Comment> {
    return this.http.post<Comment>(`${this.base}/tasks/${taskId}/comments`, req);
  }

  editComment(commentId: string, req: EditCommentRequest): Observable<Comment> {
    return this.http.put<Comment>(`${this.base}/comments/${commentId}`, req);
  }

  deleteComment(commentId: string): Observable<unknown> {
    return this.http.delete(`${this.base}/comments/${commentId}`);
  }
}
