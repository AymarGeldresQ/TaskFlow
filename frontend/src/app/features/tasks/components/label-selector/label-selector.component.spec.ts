import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/angular';
import { LabelSelectorComponent } from './label-selector.component';
import { type Label } from '../../../../shared/models';

const makeLabel = (id: string, name: string): Label => ({
  id,
  name,
  color: '#FF0000',
  projectId: 'proj-1',
});

describe('LabelSelectorComponent — Scenario D1: label toggle', () => {
  const availableLabels: Label[] = [
    makeLabel('label-1', 'bug'),
    makeLabel('label-2', 'feature'),
  ];

  it('renders all available labels as chips', async () => {
    await render(LabelSelectorComponent, {
      componentInputs: {
        availableLabels,
        attachedLabelIds: [],
      },
    });

    expect(screen.getByText('bug')).toBeTruthy();
    expect(screen.getByText('feature')).toBeTruthy();
  });

  it('emits toggle event with attached=false when clicking an unattached label', async () => {
    const events: { labelId: string; attached: boolean }[] = [];
    await render(LabelSelectorComponent, {
      componentInputs: {
        availableLabels,
        attachedLabelIds: [],
      },
      on: {
        toggle: (e: { labelId: string; attached: boolean }) => events.push(e),
      },
    });

    fireEvent.click(screen.getByText('bug'));
    expect(events).toHaveLength(1);
    expect(events[0]).toEqual({ labelId: 'label-1', attached: false });
  });

  it('emits toggle event with attached=true when clicking an attached label', async () => {
    const events: { labelId: string; attached: boolean }[] = [];
    await render(LabelSelectorComponent, {
      componentInputs: {
        availableLabels,
        attachedLabelIds: ['label-1'],
      },
      on: {
        toggle: (e: { labelId: string; attached: boolean }) => events.push(e),
      },
    });

    fireEvent.click(screen.getByText('bug'));
    expect(events).toHaveLength(1);
    expect(events[0]).toEqual({ labelId: 'label-1', attached: true });
  });
});
