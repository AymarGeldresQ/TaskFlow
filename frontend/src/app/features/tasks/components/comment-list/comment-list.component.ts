import { Component, input, output } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { type Comment } from '../../../../shared/models';

@Component({
  selector: 'tf-comment-list',
  standalone: true,
  imports: [SlicePipe, MatButtonModule],
  template: `
    <div class="comment-list">
      @for (comment of comments(); track comment.id) {
        <div class="comment-list__item">
          <div class="comment-list__meta">
            <span class="comment-list__author">{{ comment.authorName }}</span>
            <span class="comment-list__date">{{ comment.createdAt | slice: 0 : 10 }}</span>
          </div>
          <p class="comment-list__body">{{ comment.content }}</p>
          @if (comment.authorId === currentUserId()) {
            <div class="comment-list__actions">
              <button mat-button (click)="editComment.emit({ id: comment.id, body: comment.content })">
                Edit
              </button>
              <button mat-button color="warn" (click)="deleteComment.emit(comment.id)">
                Delete
              </button>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      .comment-list__item {
        padding: 12px 0;
        border-bottom: 1px solid #e0e0e0;
        &:last-child {
          border-bottom: none;
        }
      }
      .comment-list__meta {
        display: flex;
        gap: 8px;
        align-items: center;
        margin-bottom: 4px;
      }
      .comment-list__author {
        font-weight: 600;
        font-size: 0.875rem;
      }
      .comment-list__date {
        font-size: 0.75rem;
        color: #757575;
      }
      .comment-list__body {
        margin: 0;
        font-size: 0.875rem;
        line-height: 1.5;
      }
      .comment-list__actions {
        margin-top: 4px;
      }
    `,
  ],
})
export class CommentListComponent {
  readonly comments = input.required<Comment[]>();
  readonly currentUserId = input.required<string>();

  readonly editComment = output<{ id: string; body: string }>();
  readonly deleteComment = output<string>();
}
