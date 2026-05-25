import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TeamsApiClient } from './teams-api-client.service';
import { type Team, type TeamMember } from '../../shared/models';

describe('TeamsApiClient', () => {
  let service: TeamsApiClient;
  let httpMock: HttpTestingController;

  const mockTeam: Team = {
    id: 'team-1',
    name: 'Alpha',
    description: null,
    createdAt: '2024-01-01T00:00:00Z',
  };

  const mockMember: TeamMember = {
    userId: 'user-1',
    name: 'Alice',
    email: 'alice@example.com',
    role: 'OWNER',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TeamsApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getTeams sends GET /api/v1/teams', () => {
    service.getTeams().subscribe();
    const req = httpMock.expectOne('/api/v1/teams');
    expect(req.request.method).toBe('GET');
    req.flush([mockTeam]);
  });

  it('createTeam sends POST /api/v1/teams', () => {
    service.createTeam({ name: 'Beta' }).subscribe();
    const req = httpMock.expectOne('/api/v1/teams');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Beta' });
    req.flush(mockTeam);
  });

  it('getMembers sends GET /api/v1/teams/:id/members', () => {
    service.getMembers('team-1').subscribe();
    const req = httpMock.expectOne('/api/v1/teams/team-1/members');
    expect(req.request.method).toBe('GET');
    req.flush([mockMember]);
  });

  it('addMember sends POST /api/v1/teams/:id/members', () => {
    service.addMember('team-1', { userId: 'user-2', role: 'MEMBER' }).subscribe();
    const req = httpMock.expectOne('/api/v1/teams/team-1/members');
    expect(req.request.method).toBe('POST');
    req.flush(mockMember);
  });

  it('removeMember sends DELETE /api/v1/teams/:id/members/:userId', () => {
    service.removeMember('team-1', 'user-2').subscribe();
    const req = httpMock.expectOne('/api/v1/teams/team-1/members/user-2');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
