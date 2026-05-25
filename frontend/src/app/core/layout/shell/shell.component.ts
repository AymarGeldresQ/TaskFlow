import { Component, inject, signal, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { NavbarComponent } from '../navbar/navbar.component';
import { SideNavComponent } from '../side-nav/side-nav.component';
import { TeamSwitcherComponent } from '../team-switcher/team-switcher.component';
import { AuthStore } from '../../auth/auth.store';
import { TeamMembershipStore } from '../../auth/team-membership.store';
import { type Team, type Project } from '../../../shared/models';

@Component({
  selector: 'tf-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    MatSidenavModule,
    NavbarComponent,
    SideNavComponent,
    TeamSwitcherComponent,
  ],
  template: `
    <tf-navbar />
    <mat-sidenav-container>
      <mat-sidenav mode="side" opened>
        <tf-team-switcher
          [teams]="teams()"
          [activeTeamId]="activeTeamId()"
          (teamChange)="onTeamChange($event)"
        />
        <tf-side-nav [projects]="projects()" />
      </mat-sidenav>
      <mat-sidenav-content>
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    mat-sidenav-container { height: calc(100vh - 64px); }
    mat-sidenav { width: 240px; padding: 8px; }
  `],
})
export class ShellComponent implements OnInit {
  protected readonly auth = inject(AuthStore);
  protected readonly membership = inject(TeamMembershipStore);

  protected readonly teams = signal<readonly Team[]>([]);
  protected readonly projects = signal<readonly Project[]>([]);
  protected readonly activeTeamId = signal<string | null>(null);

  ngOnInit(): void {
    // currentUser is already populated by AuthStore after login.
    // TeamMembership loading (full API call) happens in Group 4 (TeamsStore).
    // Shell just exposes what AuthStore has.
  }

  protected onTeamChange(teamId: string): void {
    this.activeTeamId.set(teamId);
  }
}
