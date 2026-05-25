import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { type Label, type CreateLabelRequest } from '../../shared/models';
import { LabelsApiClient } from '../../core/http/labels-api-client.service';

@Injectable({ providedIn: 'root' })
export class LabelsStore {
  private readonly api = inject(LabelsApiClient);

  readonly labels = signal<Label[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // Tracks the last loaded projectId for reload after mutations
  private _lastProjectId: string | null = null;

  async loadLabels(projectId: string): Promise<void> {
    this._lastProjectId = projectId;
    this.loading.set(true);
    this.error.set(null);
    try {
      const labels = await firstValueFrom(this.api.getLabels(projectId));
      this.labels.set(labels);
    } catch (e: unknown) {
      this.error.set(e instanceof Error ? e.message : 'Failed to load labels');
    } finally {
      this.loading.set(false);
    }
  }

  async createLabel(projectId: string, req: CreateLabelRequest): Promise<void> {
    await firstValueFrom(this.api.createLabel(projectId, req));
    await this.loadLabels(projectId);
  }

  async deleteLabel(labelId: string): Promise<void> {
    // Optimistic removal
    this.labels.update((current) => current.filter((l) => l.id !== labelId));
    await firstValueFrom(this.api.deleteLabel(labelId));
  }
}
