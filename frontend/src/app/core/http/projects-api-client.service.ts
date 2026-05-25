import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable } from 'rxjs';
import { type Project, type CreateProjectRequest } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class ProjectsApiClient {
  private readonly http = inject(HttpClient);

  getProjects(teamId: string): Observable<Project[]> {
    return this.http.get<Project[]>(`/api/v1/teams/${teamId}/projects`);
  }

  createProject(teamId: string, req: CreateProjectRequest): Observable<Project> {
    return this.http.post<Project>(`/api/v1/teams/${teamId}/projects`, req);
  }
}
