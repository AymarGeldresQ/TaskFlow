import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TeamsStore } from './teams.store';
import { TeamsApiClient } from '../../core/http/teams-api-client.service';
import { TeamMembershipStore } from '../../core/auth/team-membership.store';
import { AuthStore } from '../../core/auth/auth.store';
import { type Team, type TeamMember } from '../../shared/models';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

describe('TeamsStore', () => {
  let store: TeamsStore;
  let apiMock: Partial<TeamsApiClient>;
  let membershipStore: TeamMembershipStore;
  let authStore: AuthStore;

  const mockTeams: Team[] = [
    { id: 'team-1', name: 'Alpha', description: null, createdAt: '2024-01-01T00:00:00Z' },
    { id: 'team-2', name: 'Beta', description: null, createdAt: '2024-01-02T00:00:00Z' },
  ];

  const mockMembers: TeamMember[] = [
    { userId: 'user-1', name: 'Alice', email: 'alice@example.com', role: 'OWNER' },
    { userId: 'user-2', name: 'Bob', email: 'bob@example.com', role: 'MEMBER' },
  ];

  beforeEach(() => {
    apiMock = {
      getTeams: vi.fn().mockReturnValue(of(mockTeams)),
      createTeam: vi.fn().mockReturnValue(of(mockTeams[0])),
      getMembers: vi.fn().mockReturnValue(of(mockMembers)),
      addMember: vi.fn().mockReturnValue(of(mockMembers[1])),
      removeMember: vi.fn().mockReturnValue(of(undefined)),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: TeamsApiClient, useValue: apiMock },
      ],
    });

    store = TestBed.inject(TeamsStore);
    membershipStore = TestBed.inject(TeamMembershipStore);
    authStore = TestBed.inject(AuthStore);
  });

  it('loadTeams populates the teams signal', async () => {
    expect(store.teams()).toEqual([]);
    await store.loadTeams();
    expect(store.teams()).toEqual(mockTeams);
  });

  it('loadTeams sets loading=true then false', async () => {
    const loadingStates: boolean[] = [];
    const origSet = store.loading.set.bind(store.loading);
    vi.spyOn(store.loading, 'set').mockImplementation((v) => {
      loadingStates.push(v);
      origSet(v);
    });
    await store.loadTeams();
    expect(loadingStates).toContain(true);
    expect(store.loading()).toBe(false);
  });

  it('loadTeams sets error signal on failure', async () => {
    (apiMock.getTeams as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('Network error')),
    );
    await store.loadTeams();
    expect(store.error()).toBeTruthy();
  });

  it('setActiveTeam sets the activeTeam signal', async () => {
    await store.loadTeams();
    await store.setActiveTeam('team-1');
    expect(store.activeTeam()?.id).toBe('team-1');
  });

  it('setActiveTeam populates TeamMembershipStore for current user', async () => {
    authStore.currentUser.set({ id: 'user-1', email: 'alice@example.com', name: 'Alice' });
    await store.loadTeams();
    await store.setActiveTeam('team-1');
    expect(membershipStore.roleFor('team-1')).toBe('OWNER');
  });

  it('setActiveTeam loads members into members signal', async () => {
    await store.loadTeams();
    await store.setActiveTeam('team-1');
    expect(store.members()).toEqual(mockMembers);
  });

  it('createTeam calls api.createTeam and reloads teams', async () => {
    await store.createTeam({ name: 'Gamma' });
    expect(apiMock.createTeam).toHaveBeenCalledWith({ name: 'Gamma' });
    expect(apiMock.getTeams).toHaveBeenCalled();
  });
});
