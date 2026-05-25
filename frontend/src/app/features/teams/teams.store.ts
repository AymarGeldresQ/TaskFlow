import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { type Team, type TeamMember, type CreateTeamRequest, type AddMemberRequest } from '../../shared/models';
import { TeamsApiClient } from '../../core/http/teams-api-client.service';
import { TeamMembershipStore } from '../../core/auth/team-membership.store';
import { AuthStore } from '../../core/auth/auth.store';

@Injectable({ providedIn: 'root' })
export class TeamsStore {
  private readonly api = inject(TeamsApiClient);
  private readonly membershipStore = inject(TeamMembershipStore);
  private readonly authStore = inject(AuthStore);

  readonly teams = signal<Team[]>([]);
  readonly activeTeam = signal<Team | null>(null);
  readonly members = signal<TeamMember[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async loadTeams(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const teams = await firstValueFrom(this.api.getTeams());
      this.teams.set(teams);
    } catch (e: unknown) {
      this.error.set(e instanceof Error ? e.message : 'Failed to load teams');
    } finally {
      this.loading.set(false);
    }
  }

  async createTeam(req: CreateTeamRequest): Promise<void> {
    await firstValueFrom(this.api.createTeam(req));
    await this.loadTeams();
  }

  async setActiveTeam(teamId: string): Promise<void> {
    const team = this.teams().find((t) => t.id === teamId) ?? null;
    this.activeTeam.set(team);

    const members = await firstValueFrom(this.api.getMembers(teamId));
    this.members.set(members);

    const currentUserId = this.authStore.currentUser()?.id;
    const myMembership = members.find((m) => m.userId === currentUserId);
    if (myMembership) {
      this.membershipStore.setMembership(teamId, myMembership.role);
    }
  }

  async loadMembers(teamId: string): Promise<void> {
    const members = await firstValueFrom(this.api.getMembers(teamId));
    this.members.set(members);
  }

  async addMember(teamId: string, req: AddMemberRequest): Promise<void> {
    await firstValueFrom(this.api.addMember(teamId, req));
    await this.loadMembers(teamId);
  }

  async removeMember(teamId: string, userId: string): Promise<void> {
    await firstValueFrom(this.api.removeMember(teamId, userId));
    await this.loadMembers(teamId);
  }
}
