import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthApiClient } from './auth-api-client.service';

describe('AuthApiClient', () => {
  let service: AuthApiClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthApiClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('sends POST to /api/v1/auth/login with body', () => {
    service.login({ email: 'a@b.com', password: 'pass' }).subscribe();
    const req = httpMock.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'a@b.com', password: 'pass' });
    req.flush({ accessToken: 'at', refreshToken: 'rt', user: { id: '1', email: 'a@b.com', name: 'A' } });
  });

  it('sends POST to /api/v1/auth/register with body', () => {
    service.register({ email: 'a@b.com', password: 'pass', name: 'A' }).subscribe();
    const req = httpMock.expectOne('/api/v1/auth/register');
    expect(req.request.method).toBe('POST');
    req.flush({ accessToken: 'at', refreshToken: 'rt', user: { id: '1', email: 'a@b.com', name: 'A' } });
  });

  it('sends POST to /api/v1/auth/refresh with body', () => {
    service.refresh({ refreshToken: 'rt' }).subscribe();
    const req = httpMock.expectOne('/api/v1/auth/refresh');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ refreshToken: 'rt' });
    req.flush({ accessToken: 'at', refreshToken: 'rt2', user: { id: '1', email: 'a@b.com', name: 'A' } });
  });

  it('sends POST to /api/v1/auth/logout', () => {
    service.logout().subscribe();
    const req = httpMock.expectOne('/api/v1/auth/logout');
    expect(req.request.method).toBe('POST');
    req.flush(null);
  });
});
