import { Injectable, signal } from '@angular/core';
import { type TeamRole } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class TeamMembershipStore {
  private readonly memberships = signal<Map<string, TeamRole>>(new Map());

  roleFor(teamId: string): TeamRole | null {
    return this.memberships().get(teamId) ?? null;
  }

  setMembership(teamId: string, role: TeamRole): void {
    this.memberships.update((map) => {
      const updated = new Map(map);
      updated.set(teamId, role);
      return updated;
    });
  }

  clearMemberships(): void {
    this.memberships.set(new Map());
  }
}
