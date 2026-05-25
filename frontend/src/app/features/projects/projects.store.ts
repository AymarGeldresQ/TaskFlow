import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { type Project, type CreateProjectRequest } from '../../shared/models';
import { ProjectsApiClient } from '../../core/http/projects-api-client.service';

@Injectable({ providedIn: 'root' })
export class ProjectsStore {
  private readonly api = inject(ProjectsApiClient);

  readonly projects = signal<Project[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async loadProjects(teamId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const projects = await firstValueFrom(this.api.getProjects(teamId));
      this.projects.set(projects);
    } catch (e: unknown) {
      this.error.set(e instanceof Error ? e.message : 'Failed to load projects');
    } finally {
      this.loading.set(false);
    }
  }

  async createProject(teamId: string, req: CreateProjectRequest): Promise<void> {
    await firstValueFrom(this.api.createProject(teamId, req));
    await this.loadProjects(teamId);
  }
}
