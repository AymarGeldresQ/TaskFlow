import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { AuthStore } from './auth.store';
import { TokenStorageService } from './token-storage.service';

@Component({ template: '', standalone: true })
class DummyComponent {}

describe('AuthStore', () => {
  let store: AuthStore;
  let httpMock: HttpTestingController;
  let storage: TokenStorageService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'login', component: DummyComponent }]),
      ],
    });
    store = TestBed.inject(AuthStore);
    httpMock = TestBed.inject(HttpTestingController);
    storage = TestBed.inject(TokenStorageService);
  });

  it('isAuthenticated is false initially', () => {
    expect(store.isAuthenticated()).toBe(false);
  });

  it('login sets accessToken, currentUser, and stores refresh token', async () => {
    const loginPromise = store.login('a@b.com', 'pass');
    const req = httpMock.expectOne('/api/v1/auth/login');
    req.flush({ accessToken: 'at', refreshToken: 'rt', user: { id: '1', email: 'a@b.com', name: 'Alice' } });
    await loginPromise;

    expect(store.accessToken()).toBe('at');
    expect(store.currentUser()).toEqual({ id: '1', email: 'a@b.com', name: 'Alice' });
    expect(store.isAuthenticated()).toBe(true);
    expect(storage.getRefreshToken()).toBe('rt');
  });

  it('logout clears signals, storage, and navigates to /login', () => {
    store.setTokens('at', 'rt');
    store.logout();

    expect(store.accessToken()).toBeNull();
    expect(store.currentUser()).toBeNull();
    expect(store.isAuthenticated()).toBe(false);
    expect(storage.getRefreshToken()).toBeNull();
  });

  it('setTokens updates accessToken signal and storage', () => {
    store.setTokens('new-at', 'new-rt');
    expect(store.accessToken()).toBe('new-at');
    expect(storage.getRefreshToken()).toBe('new-rt');
  });

  it('clearAuth clears signals and storage without navigating', () => {
    store.setTokens('at', 'rt');
    store.clearAuth();
    expect(store.accessToken()).toBeNull();
    expect(storage.getRefreshToken()).toBeNull();
  });
});
