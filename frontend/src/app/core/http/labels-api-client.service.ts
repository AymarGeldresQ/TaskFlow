import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable } from 'rxjs';
import { type Label, type CreateLabelRequest } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class LabelsApiClient {
  private readonly http = inject(HttpClient);

  getLabels(projectId: string): Observable<Label[]> {
    return this.http.get<Label[]>(`/api/v1/projects/${projectId}/labels`);
  }

  createLabel(projectId: string, req: CreateLabelRequest): Observable<Label> {
    return this.http.post<Label>(`/api/v1/projects/${projectId}/labels`, req);
  }

  deleteLabel(labelId: string): Observable<unknown> {
    return this.http.delete(`/api/v1/labels/${labelId}`);
  }
}
