import { Component, input, output } from '@angular/core';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { type TeamMember } from '../../../../shared/models';

@Component({
  selector: 'tf-member-list',
  standalone: true,
  imports: [MatListModule, MatIconModule, MatButtonModule],
  template: `
    <mat-list>
      @for (member of members(); track member.userId) {
        <mat-list-item>
          <span matListItemTitle>{{ member.name }}</span>
          <span matListItemLine>{{ member.email }} · {{ member.role }}</span>
          @if (canManage()) {
            <button
              mat-icon-button
              matListItemMeta
              aria-label="Remove member"
              (click)="removeMember.emit(member.userId)"
            >
              <mat-icon>person_remove</mat-icon>
            </button>
          }
        </mat-list-item>
      }
    </mat-list>
  `,
})
export class MemberListComponent {
  readonly members = input.required<TeamMember[]>();
  readonly canManage = input<boolean>(false);
  readonly removeMember = output<string>();
}
