import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthStore } from '../../auth/auth.store';

@Component({
  selector: 'tf-navbar',
  standalone: true,
  imports: [RouterLink, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary" role="navigation">
      <a routerLink="/teams" class="app-name">TaskFlow</a>
      <span class="spacer"></span>
      @if (auth.currentUser(); as user) {
        <span class="user-name">{{ user.name }}</span>
      }
      <button mat-icon-button (click)="auth.logout()" aria-label="Logout">
        <mat-icon>logout</mat-icon>
      </button>
    </mat-toolbar>
  `,
  styles: [`
    .spacer { flex: 1 1 auto; }
    .app-name { color: inherit; text-decoration: none; font-weight: 600; }
    .user-name { margin-right: 8px; }
  `],
})
export class NavbarComponent {
  protected readonly auth = inject(AuthStore);
}
