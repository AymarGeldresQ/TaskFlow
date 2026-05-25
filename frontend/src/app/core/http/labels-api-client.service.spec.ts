import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { LabelsApiClient } from './labels-api-client.service';
import { type Label } from '../../shared/models';

describe('LabelsApiClient', () => {
  let service: LabelsApiClient;
  let httpMock: HttpTestingController;

  const mockLabel: Label = {
    id: 'label-1',
    name: 'bug',
    color: '#FF0000',
    projectId: 'proj-1',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LabelsApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getLabels sends GET /api/v1/projects/:projectId/labels', () => {
    service.getLabels('proj-1').subscribe();
    const req = httpMock.expectOne('/api/v1/projects/proj-1/labels');
    expect(req.request.method).toBe('GET');
    req.flush([mockLabel]);
  });

  it('createLabel sends POST /api/v1/projects/:projectId/labels', () => {
    service.createLabel('proj-1', { name: 'feature', color: '#00FF00' }).subscribe();
    const req = httpMock.expectOne('/api/v1/projects/proj-1/labels');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'feature', color: '#00FF00' });
    req.flush(mockLabel);
  });

  it('deleteLabel sends DELETE /api/v1/labels/:labelId', () => {
    service.deleteLabel('label-1').subscribe();
    const req = httpMock.expectOne('/api/v1/labels/label-1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
