import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ProjectsApiClient } from './projects-api-client.service';
import { type Project } from '../../shared/models';

describe('ProjectsApiClient', () => {
  let service: ProjectsApiClient;
  let httpMock: HttpTestingController;

  const mockProject: Project = {
    id: 'proj-1',
    name: 'Orbit',
    description: null,
    teamId: 'team-1',
    createdAt: '2024-01-01T00:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjectsApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getProjects sends GET /api/v1/teams/:teamId/projects', () => {
    service.getProjects('team-1').subscribe();
    const req = httpMock.expectOne('/api/v1/teams/team-1/projects');
    expect(req.request.method).toBe('GET');
    req.flush([mockProject]);
  });

  it('createProject sends POST /api/v1/teams/:teamId/projects', () => {
    service.createProject('team-1', { name: 'Nova', teamId: 'team-1' }).subscribe();
    const req = httpMock.expectOne('/api/v1/teams/team-1/projects');
    expect(req.request.method).toBe('POST');
    req.flush(mockProject);
  });
});
