import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { catchError, firstValueFrom, of } from 'rxjs';
import { errorInterceptor } from './error.interceptor';
import { ApiError } from './api-error';

describe('errorInterceptor', () => {
  let httpMock: HttpTestingController;
  let http: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    http = TestBed.inject(HttpClient);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('maps 400 HttpErrorResponse to ApiError with message', async () => {
    const result = firstValueFrom(
      http.get<unknown>('/api/v1/resource').pipe(catchError((err: unknown) => of(err)))
    );

    httpMock.expectOne('/api/v1/resource').flush(
      { message: 'Validation failed' },
      { status: 400, statusText: 'Bad Request' }
    );

    const err = await result;
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).status).toBe(400);
    expect((err as ApiError).message).toBe('Validation failed');
  });

  it('maps 500 HttpErrorResponse to ApiError', async () => {
    const result = firstValueFrom(
      http.get<unknown>('/api/v1/resource').pipe(catchError((err: unknown) => of(err)))
    );

    httpMock.expectOne('/api/v1/resource').flush(
      { message: 'Internal server error' },
      { status: 500, statusText: 'Internal Server Error' }
    );

    const err = await result;
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).status).toBe(500);
  });

  it('uses fallback message when no message in body', async () => {
    const result = firstValueFrom(
      http.get<unknown>('/api/v1/resource').pipe(catchError((err: unknown) => of(err)))
    );

    httpMock.expectOne('/api/v1/resource').flush(null, { status: 503, statusText: 'Service Unavailable' });

    const err = await result;
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).message).toContain('503');
  });

  it('passes successful responses through unmodified', async () => {
    const result = firstValueFrom(http.get<{ id: string }>('/api/v1/resource'));

    httpMock.expectOne('/api/v1/resource').flush({ id: '123' });

    const data = await result;
    expect(data).toEqual({ id: '123' });
  });
});
