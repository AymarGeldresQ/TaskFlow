import { describe, it, expect } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { render } from '@testing-library/angular';
import { BoardColumnComponent } from './board-column.component';
import { TaskStatus } from '../../../../shared/models/task-status';
import { type Task } from '../../../../shared/models';
import { type CdkDrag } from '@angular/cdk/drag-drop';

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

describe('BoardColumnComponent', () => {
  it('renders the column status heading', async () => {
    const { getByText } = await render(BoardColumnComponent, {
      inputs: {
        status: TaskStatus.TODO,
        tasks: [makeTask('t1', TaskStatus.TODO)],
      },
    });
    expect(getByText('TODO')).toBeTruthy();
  });

  it('renders task cards for each task in the column', async () => {
    const tasks = [makeTask('t1', TaskStatus.TODO), makeTask('t2', TaskStatus.TODO)];
    const { getByText } = await render(BoardColumnComponent, {
      inputs: {
        status: TaskStatus.TODO,
        tasks,
      },
    });
    expect(getByText('Task t1')).toBeTruthy();
    expect(getByText('Task t2')).toBeTruthy();
  });

  // R10 verification: canDrop predicate blocks invalid transitions
  it('canDrop returns false for IN_REVIEW → BACKLOG (invalid transition)', async () => {
    const { fixture } = await render(BoardColumnComponent, {
      inputs: {
        status: TaskStatus.BACKLOG,
        tasks: [],
      },
    });
    const component = fixture.componentInstance;

    const mockDrag = {
      data: makeTask('t1', TaskStatus.IN_REVIEW),
    } as CdkDrag<Task>;

    expect(component.canDrop(mockDrag)).toBe(false);
  });

  // R10 verification: canDrop allows valid transitions
  it('canDrop returns true for TODO → IN_PROGRESS (valid transition)', async () => {
    const { fixture } = await render(BoardColumnComponent, {
      inputs: {
        status: TaskStatus.IN_PROGRESS,
        tasks: [],
      },
    });
    const component = fixture.componentInstance;

    const mockDrag = {
      data: makeTask('t1', TaskStatus.TODO),
    } as CdkDrag<Task>;

    expect(component.canDrop(mockDrag)).toBe(true);
  });
});
