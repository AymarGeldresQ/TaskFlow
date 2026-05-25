import { Component, input, output } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { type Label } from '../../../../shared/models';

@Component({
  selector: 'tf-label-selector',
  standalone: true,
  imports: [MatChipsModule],
  template: `
    <mat-chip-set class="label-selector">
      @for (label of availableLabels(); track label.id) {
        <mat-chip
          [class.label-selector__chip--attached]="isAttached(label.id)"
          [style.background-color]="isAttached(label.id) ? label.color : undefined"
          (click)="onToggle(label.id)"
        >
          {{ label.name }}
        </mat-chip>
      }
    </mat-chip-set>
  `,
  styles: [
    `
      .label-selector {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
      }
      mat-chip {
        cursor: pointer;
      }
      .label-selector__chip--attached {
        color: white;
        font-weight: 600;
      }
    `,
  ],
})
export class LabelSelectorComponent {
  readonly availableLabels = input.required<Label[]>();
  readonly attachedLabelIds = input.required<string[]>();

  readonly toggle = output<{ labelId: string; attached: boolean }>();

  protected isAttached(labelId: string): boolean {
    return this.attachedLabelIds().includes(labelId);
  }

  protected onToggle(labelId: string): void {
    this.toggle.emit({ labelId, attached: this.isAttached(labelId) });
  }
}
