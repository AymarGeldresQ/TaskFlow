import { Routes } from '@angular/router';
import { authGuard } from './core/auth/guards/auth.guard';
import { unauthGuard } from './core/auth/guards/unauth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'teams' },

  {
    path: 'login',
    canMatch: [unauthGuard],
    loadComponent: () =>
      import('./features/auth/login/login-page.component').then(
        (m) => m.LoginPageComponent,
      ),
  },

  {
    path: 'register',
    canMatch: [unauthGuard],
    loadComponent: () =>
      import('./features/auth/register/register-page.component').then(
        (m) => m.RegisterPageComponent,
      ),
  },

  {
    path: '',
    canMatch: [authGuard],
    loadComponent: () =>
      import('./core/layout/shell/shell.component').then(
        (m) => m.ShellComponent,
      ),
    children: [
      {
        path: 'teams',
        loadComponent: () =>
          import('./features/teams/team-list/team-list-page.component').then(
            (m) => m.TeamListPageComponent,
          ),
      },
      {
        path: 'teams/:teamId',
        loadComponent: () =>
          import('./features/teams/team-detail/team-detail-page.component').then(
            (m) => m.TeamDetailPageComponent,
          ),
      },
      {
        path: 'teams/:teamId/projects/:projectId/board',
        loadComponent: () =>
          import('./features/board/board-page/board-page.component').then(
            (m) => m.BoardPageComponent,
          ),
      },
    ],
  },

  { path: '**', redirectTo: 'teams' },
];
