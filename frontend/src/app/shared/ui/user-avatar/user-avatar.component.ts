import { Component, computed, input } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { type User } from '../../../shared/models';

@Component({
  selector: 'tf-user-avatar',
  standalone: true,
  imports: [MatTooltipModule],
  template: `
    <span
      class="user-avatar"
      [matTooltip]="user()?.name ?? ''"
      [attr.aria-label]="user()?.name ?? 'Unknown user'"
    >
      {{ initials() }}
    </span>
  `,
  styles: [
    `
      .user-avatar {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: #5c6bc0;
        color: white;
        font-size: 0.7rem;
        font-weight: 600;
        cursor: default;
      }
    `,
  ],
})
export class UserAvatarComponent {
  readonly user = input<User | null>(null);

  protected readonly initials = computed((): string => {
    const name = this.user()?.name ?? '';
    return name
      .split(' ')
      .filter((p) => p.length > 0)
      .slice(0, 2)
      .map((p) => p[0].toUpperCase())
      .join('');
  });
}
