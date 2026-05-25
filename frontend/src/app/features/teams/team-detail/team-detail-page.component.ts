import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { TeamsStore } from '../teams.store';
import { AuthStore } from '../../../core/auth/auth.store';
import { TeamMembershipStore } from '../../../core/auth/team-membership.store';
import { MemberListComponent } from '../components/member-list/member-list.component';
import { InviteMemberDialogComponent } from '../components/invite-member-dialog/invite-member-dialog.component';
import { type AddMemberRequest } from '../../../shared/models';

@Component({
  selector: 'tf-team-detail-page',
  standalone: true,
  imports: [MatButtonModule, MatProgressSpinnerModule, MatIconModule, MemberListComponent],
  template: `
    <div class="page-header">
      <h1>{{ store.activeTeam()?.name ?? 'Team' }}</h1>
      @if (canManage()) {
        <button mat-flat-button color="primary" (click)="openInviteDialog()">
          <mat-icon>person_add</mat-icon>
          Add Member
        </button>
      }
    </div>

    @if (store.loading()) {
      <mat-spinner diameter="40" />
    } @else {
      <tf-member-list
        [members]="store.members()"
        [canManage]="canManage()"
        (removeMember)="onRemoveMember($event)"
      />
    }
  `,
  styles: [`
    .page-header { display: flex; justify-content: space-between; align-items: center; padding: 16px; }
  `],
})
export class TeamDetailPageComponent implements OnInit {
  protected readonly store = inject(TeamsStore);
  private readonly authStore = inject(AuthStore);
  private readonly membershipStore = inject(TeamMembershipStore);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  protected canManage(): boolean {
    const teamId = this.teamId();
    if (!teamId) return false;
    const role = this.membershipStore.roleFor(teamId);
    return role === 'OWNER' || role === 'ADMIN';
  }

  ngOnInit(): void {
    const teamId = this.teamId();
    if (teamId) {
      void this.store.setActiveTeam(teamId);
    }
  }

  private teamId(): string | null {
    return this.route.snapshot.paramMap.get('teamId');
  }

  protected openInviteDialog(): void {
    const ref = this.dialog.open(InviteMemberDialogComponent, { width: '400px' });
    ref.afterClosed().subscribe((result: AddMemberRequest | undefined) => {
      const teamId = this.teamId();
      if (result && teamId) {
        void this.store.addMember(teamId, result);
      }
    });
  }

  protected onRemoveMember(userId: string): void {
    const teamId = this.teamId();
    if (teamId) {
      void this.store.removeMember(teamId, userId);
    }
  }
}
