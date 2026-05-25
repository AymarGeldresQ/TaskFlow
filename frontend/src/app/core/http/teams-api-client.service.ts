import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable } from 'rxjs';
import {
  type Team,
  type TeamMember,
  type CreateTeamRequest,
  type AddMemberRequest,
} from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class TeamsApiClient {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/teams';

  getTeams(): Observable<Team[]> {
    return this.http.get<Team[]>(this.base);
  }

  createTeam(req: CreateTeamRequest): Observable<Team> {
    return this.http.post<Team>(this.base, req);
  }

  getMembers(teamId: string): Observable<TeamMember[]> {
    return this.http.get<TeamMember[]>(`${this.base}/${teamId}/members`);
  }

  addMember(teamId: string, req: AddMemberRequest): Observable<TeamMember> {
    return this.http.post<TeamMember>(`${this.base}/${teamId}/members`, req);
  }

  removeMember(teamId: string, userId: string): Observable<unknown> {
    return this.http.delete(`${this.base}/${teamId}/members/${userId}`);
  }
}
