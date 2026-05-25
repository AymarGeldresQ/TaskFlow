import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ProjectsStore } from './projects.store';
import { ProjectsApiClient } from '../../core/http/projects-api-client.service';
import { type Project } from '../../shared/models';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

describe('ProjectsStore', () => {
  let store: ProjectsStore;
  let apiMock: Partial<ProjectsApiClient>;

  const mockProjects: Project[] = [
    { id: 'proj-1', name: 'Orbit', description: null, teamId: 'team-1', createdAt: '2024-01-01T00:00:00Z' },
    { id: 'proj-2', name: 'Nova', description: null, teamId: 'team-1', createdAt: '2024-01-02T00:00:00Z' },
  ];

  beforeEach(() => {
    apiMock = {
      getProjects: vi.fn().mockReturnValue(of(mockProjects)),
      createProject: vi.fn().mockReturnValue(of(mockProjects[0])),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ProjectsApiClient, useValue: apiMock },
      ],
    });

    store = TestBed.inject(ProjectsStore);
  });

  it('projects signal starts empty', () => {
    expect(store.projects()).toEqual([]);
  });

  it('loadProjects populates the projects signal', async () => {
    await store.loadProjects('team-1');
    expect(store.projects()).toEqual(mockProjects);
  });

  it('loadProjects sets loading to true then false', async () => {
    const loadingStates: boolean[] = [];
    const origSet = store.loading.set.bind(store.loading);
    vi.spyOn(store.loading, 'set').mockImplementation((v) => {
      loadingStates.push(v);
      origSet(v);
    });
    await store.loadProjects('team-1');
    expect(loadingStates).toContain(true);
    expect(store.loading()).toBe(false);
  });

  it('createProject calls api.createProject then reloads', async () => {
    await store.createProject('team-1', { name: 'Pulsar', teamId: 'team-1' });
    expect(apiMock.createProject).toHaveBeenCalledWith('team-1', { name: 'Pulsar', teamId: 'team-1' });
    expect(apiMock.getProjects).toHaveBeenCalledWith('team-1');
  });

  it('loadProjects sets error on failure', async () => {
    (apiMock.getProjects as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('fail')),
    );
    await store.loadProjects('team-1');
    expect(store.error()).toBeTruthy();
  });
});
