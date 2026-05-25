import { Component, input } from '@angular/core';
import { NgClass } from '@angular/common';
import { type TaskPriority } from '../../models';

const PRIORITY_CLASSES: Record<TaskPriority, string> = {
  LOW: 'priority-low',
  MEDIUM: 'priority-medium',
  HIGH: 'priority-high',
  CRITICAL: 'priority-critical',
};

@Component({
  selector: 'tf-priority-chip',
  standalone: true,
  imports: [NgClass],
  template: `
    <span class="priority-chip" [ngClass]="priorityClass()">{{ priority() }}</span>
  `,
  styles: [
    `
      .priority-chip {
        display: inline-block;
        padding: 2px 8px;
        border-radius: 12px;
        font-size: 0.7rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }
      .priority-low {
        background: #e8f5e9;
        color: #388e3c;
      }
      .priority-medium {
        background: #fff8e1;
        color: #f57f17;
      }
      .priority-high {
        background: #fff3e0;
        color: #e65100;
      }
      .priority-critical {
        background: #fce4ec;
        color: #c62828;
      }
    `,
  ],
})
export class PriorityChipComponent {
  readonly priority = input.required<TaskPriority>();

  readonly priorityClass = (): string => PRIORITY_CLASSES[this.priority()];
}
