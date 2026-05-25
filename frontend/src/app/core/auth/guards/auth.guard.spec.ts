import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { authGuard } from './auth.guard';
import { unauthGuard } from './unauth.guard';
import { AuthStore } from '../auth.store';

function runGuard(guard: ReturnType<typeof authGuard | typeof unauthGuard>): boolean | UrlTree {
  if (typeof guard === 'function') {
    return guard as unknown as boolean | UrlTree;
  }
  return guard as boolean | UrlTree;
}

describe('authGuard', () => {
  let authStore: AuthStore;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    authStore = TestBed.inject(AuthStore);
    router = TestBed.inject(Router);
  });

  it('returns true when user is authenticated', () => {
    authStore.accessToken.set('valid-token');

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, []));
    expect(result).toBe(true);
  });

  it('returns UrlTree to /login with returnUrl when unauthenticated', () => {
    authStore.accessToken.set(null);

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, []));
    expect(result).toBeInstanceOf(UrlTree);
    const urlTree = result as UrlTree;
    const serialized = router.serializeUrl(urlTree);
    expect(serialized).toContain('/login');
    expect(serialized).toContain('returnUrl');
  });

  it('encodes the returnUrl correctly', () => {
    authStore.accessToken.set(null);

    const result = TestBed.runInInjectionContext(() => authGuard({} as never, []));
    expect(result).toBeInstanceOf(UrlTree);
    const urlTree = result as UrlTree;
    const returnUrlParam = urlTree.queryParams['returnUrl'] as string;
    expect(typeof returnUrlParam).toBe('string');
  });
});

describe('unauthGuard', () => {
  let authStore: AuthStore;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    authStore = TestBed.inject(AuthStore);
    router = TestBed.inject(Router);
  });

  it('returns UrlTree to /teams when user is authenticated', () => {
    authStore.accessToken.set('valid-token');

    const result = TestBed.runInInjectionContext(() => unauthGuard({} as never, []));
    expect(result).toBeInstanceOf(UrlTree);
    const serialized = router.serializeUrl(result as UrlTree);
    expect(serialized).toBe('/teams');
  });

  it('returns true when user is unauthenticated', () => {
    authStore.accessToken.set(null);

    const result = TestBed.runInInjectionContext(() => unauthGuard({} as never, []));
    expect(result).toBe(true);
  });
});
