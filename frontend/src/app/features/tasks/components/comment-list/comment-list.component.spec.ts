import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/angular';
import { CommentListComponent } from './comment-list.component';
import { type Comment } from '../../../../shared/models';

const makeComment = (id: string, authorId: string, content: string): Comment => ({
  id,
  taskId: 'task-1',
  authorId,
  authorName: authorId === 'user-me' ? 'Me' : 'Other User',
  content,
  createdAt: '2024-01-01T00:00:00Z',
  updatedAt: '2024-01-01T00:00:00Z',
});

describe('CommentListComponent — Scenario F1: comment ownership', () => {
  const myComment = makeComment('c1', 'user-me', 'My comment text');
  const otherComment = makeComment('c2', 'user-other', 'Other comment text');

  it('shows edit and delete controls only for comments by the current user', async () => {
    await render(CommentListComponent, {
      componentInputs: {
        comments: [myComment, otherComment],
        currentUserId: 'user-me',
      },
    });

    // Both comment bodies must be visible
    expect(screen.getByText('My comment text')).toBeTruthy();
    expect(screen.getByText('Other comment text')).toBeTruthy();

    // Only one set of edit/delete buttons must exist — for current user's comment only
    const editButtons = screen.queryAllByText('Edit');
    const deleteButtons = screen.queryAllByText('Delete');
    expect(editButtons).toHaveLength(1);
    expect(deleteButtons).toHaveLength(1);
  });

  it('shows no edit/delete controls when there are no comments by the current user', async () => {
    await render(CommentListComponent, {
      componentInputs: {
        comments: [otherComment],
        currentUserId: 'user-me',
      },
    });

    expect(screen.queryAllByText('Edit')).toHaveLength(0);
    expect(screen.queryAllByText('Delete')).toHaveLength(0);
  });
});
