import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { catchError, firstValueFrom, lastValueFrom, of } from 'rxjs';
import { refreshInterceptor } from './refresh.interceptor';
import { authInterceptor } from './auth.interceptor';
import { AuthApiClient } from '../auth-api-client.service';
import { AuthStore } from '../auth.store';
import { TokenStorageService } from '../token-storage.service';
import { type AuthResponse } from '../../../shared/models';

@Component({ template: '', standalone: true })
class DummyComponent {}

describe('refreshInterceptor — single-flight', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;
  let authApi: { refresh: ReturnType<typeof vi.fn>; login: ReturnType<typeof vi.fn>; register: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };
  let authStore: AuthStore;
  let storage: TokenStorageService;

  beforeEach(() => {
    localStorage.clear();
    authApi = {
      refresh: vi.fn(),
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor, refreshInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: DummyComponent }]),
        { provide: AuthApiClient, useValue: authApi },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    http = TestBed.inject(HttpClient);
    authStore = TestBed.inject(AuthStore);
    storage = TestBed.inject(TokenStorageService);

    authStore.accessToken.set('stale-token');
    storage.setRefreshToken('valid-refresh-token');
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('fires ONE refresh call for two parallel 401s and retries both', async () => {
    const refreshSubject = new Subject<AuthResponse>();
    authApi.refresh.mockReturnValue(refreshSubject.asObservable());

    const r1 = firstValueFrom(http.get<unknown>('/api/v1/teams'));
    const r2 = firstValueFrom(http.get<unknown>('/api/v1/projects/x/tasks'));

    const req1 = httpMock.expectOne('/api/v1/teams');
    const req2 = httpMock.expectOne('/api/v1/projects/x/tasks');
    req1.flush(null, { status: 401, statusText: 'Unauthorized' });
    req2.flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(authApi.refresh).toHaveBeenCalledTimes(1);

    refreshSubject.next({ accessToken: 'new-token', refreshToken: 'new-refresh', user: { id: '1', email: 'a@b.com', name: 'A' } });
    refreshSubject.complete();

    const retry1 = httpMock.expectOne('/api/v1/teams');
    const retry2 = httpMock.expectOne('/api/v1/projects/x/tasks');
    expect(retry1.request.headers.get('Authorization')).toBe('Bearer new-token');
    expect(retry2.request.headers.get('Authorization')).toBe('Bearer new-token');
    retry1.flush({ content: [] });
    retry2.flush({ content: [] });

    await expect(r1).resolves.toBeTruthy();
    await expect(r2).resolves.toBeTruthy();
  });

  it('calls AuthStore.clearAuth when refresh coordinator errors', async () => {
    const errorSubject = new Subject<AuthResponse>();
    authApi.refresh.mockReturnValue(errorSubject.asObservable());

    const clearAuthSpy = vi.spyOn(authStore, 'clearAuth');

    const req$ = lastValueFrom(
      http.get<unknown>('/api/v1/teams').pipe(
        catchError(() => of('failed')),
      ),
      { defaultValue: 'empty' }
    );

    const req = httpMock.expectOne('/api/v1/teams');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    errorSubject.error(new HttpErrorResponse({ status: 401 }));

    const result = await req$;
    // When refresh fails, refreshInterceptor returns EMPTY — either 'empty' default or 'failed' from catchError
    expect(['failed', 'empty']).toContain(result);
    expect(clearAuthSpy).toHaveBeenCalled();
  });

  it('passes through non-401 errors without triggering refresh', async () => {
    const result = firstValueFrom(
      http.get<unknown>('/api/v1/teams').pipe(catchError((err: unknown) => of(err)))
    );

    const req = httpMock.expectOne('/api/v1/teams');
    req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

    const err = await result;
    expect(authApi.refresh).not.toHaveBeenCalled();
    expect(err).toBeInstanceOf(HttpErrorResponse);
    expect((err as HttpErrorResponse).status).toBe(500);
  });

  it('passes through 401 from auth endpoints without triggering refresh', async () => {
    const result = firstValueFrom(
      http.post<unknown>('/api/v1/auth/login', {}).pipe(catchError((err: unknown) => of(err)))
    );

    const req = httpMock.expectOne('/api/v1/auth/login');
    req.flush(null, { status: 401, statusText: 'Unauthorized' });

    const err = await result;
    expect(authApi.refresh).not.toHaveBeenCalled();
    expect(err).toBeInstanceOf(HttpErrorResponse);
  });
});
