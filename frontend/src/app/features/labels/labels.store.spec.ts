import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { LabelsStore } from './labels.store';
import { LabelsApiClient } from '../../core/http/labels-api-client.service';
import { type Label } from '../../shared/models';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('LabelsStore', () => {
  let store: LabelsStore;
  let apiMock: Partial<LabelsApiClient>;

  const mockLabels: Label[] = [
    { id: 'label-1', name: 'bug', color: '#FF0000', projectId: 'proj-1' },
    { id: 'label-2', name: 'feature', color: '#00FF00', projectId: 'proj-1' },
  ];

  beforeEach(() => {
    apiMock = {
      getLabels: vi.fn().mockReturnValue(of(mockLabels)),
      createLabel: vi.fn().mockReturnValue(of(mockLabels[0])),
      deleteLabel: vi.fn().mockReturnValue(of(undefined)),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LabelsApiClient, useValue: apiMock },
      ],
    });

    store = TestBed.inject(LabelsStore);
  });

  it('labels signal starts empty', () => {
    expect(store.labels()).toEqual([]);
  });

  it('loadLabels populates labels signal', async () => {
    await store.loadLabels('proj-1');
    expect(store.labels()).toEqual(mockLabels);
  });

  it('loadLabels sets loading to true then false', async () => {
    const states: boolean[] = [];
    const origSet = store.loading.set.bind(store.loading);
    vi.spyOn(store.loading, 'set').mockImplementation((v) => {
      states.push(v);
      origSet(v);
    });
    await store.loadLabels('proj-1');
    expect(states).toContain(true);
    expect(store.loading()).toBe(false);
  });

  it('createLabel calls api.createLabel then reloads', async () => {
    store['_lastProjectId'] = 'proj-1';
    await store.createLabel('proj-1', { name: 'urgent', color: '#FF8800' });
    expect(apiMock.createLabel).toHaveBeenCalledWith('proj-1', { name: 'urgent', color: '#FF8800' });
    expect(apiMock.getLabels).toHaveBeenCalledWith('proj-1');
  });

  it('deleteLabel removes label from signal optimistically', async () => {
    await store.loadLabels('proj-1');
    await store.deleteLabel('label-1');
    expect(store.labels().find((l) => l.id === 'label-1')).toBeUndefined();
  });

  it('loadLabels sets error on failure', async () => {
    (apiMock.getLabels as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new Error('fail')),
    );
    await store.loadLabels('proj-1');
    expect(store.error()).toBeTruthy();
  });
});
