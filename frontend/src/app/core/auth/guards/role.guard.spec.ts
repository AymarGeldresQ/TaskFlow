import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree, provideRouter, type UrlSegment } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { roleGuard } from './role.guard';
import { AuthStore } from '../auth.store';
import { TeamMembershipStore } from '../team-membership.store';

function makeSegments(paths: string[]): UrlSegment[] {
  return paths.map((path) => ({ path, parameters: {} } as UrlSegment));
}

describe('roleGuard', () => {
  let authStore: AuthStore;
  let membershipStore: TeamMembershipStore;
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
    membershipStore = TestBed.inject(TeamMembershipStore);
    router = TestBed.inject(Router);
  });

  it('returns true for OWNER role when OWNER is in allowed list', () => {
    authStore.accessToken.set('valid-token');
    membershipStore.setMembership('team-1', 'OWNER');

    const guard = roleGuard(['OWNER', 'ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as never, makeSegments(['teams', 'team-1', 'settings']))
    );
    expect(result).toBe(true);
  });

  it('returns true for ADMIN role when ADMIN is in allowed list', () => {
    authStore.accessToken.set('valid-token');
    membershipStore.setMembership('team-1', 'ADMIN');

    const guard = roleGuard(['OWNER', 'ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as never, makeSegments(['teams', 'team-1', 'settings']))
    );
    expect(result).toBe(true);
  });

  it('redirects MEMBER role to /teams (Scenario C1)', () => {
    authStore.accessToken.set('valid-token');
    membershipStore.setMembership('team-1', 'MEMBER');

    const guard = roleGuard(['OWNER', 'ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as never, makeSegments(['teams', 'team-1', 'settings']))
    );
    expect(result).toBeInstanceOf(UrlTree);
    const serialized = router.serializeUrl(result as UrlTree);
    expect(serialized).toBe('/teams');
  });

  it('redirects VIEWER role to /teams', () => {
    authStore.accessToken.set('valid-token');
    membershipStore.setMembership('team-1', 'VIEWER');

    const guard = roleGuard(['OWNER', 'ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as never, makeSegments(['teams', 'team-1', 'settings']))
    );
    expect(result).toBeInstanceOf(UrlTree);
    const serialized = router.serializeUrl(result as UrlTree);
    expect(serialized).toBe('/teams');
  });

  it('redirects unauthenticated user to /login', () => {
    authStore.accessToken.set(null);

    const guard = roleGuard(['OWNER', 'ADMIN']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as never, makeSegments(['teams', 'team-1', 'settings']))
    );
    expect(result).toBeInstanceOf(UrlTree);
    const serialized = router.serializeUrl(result as UrlTree);
    expect(serialized).toBe('/login');
  });

  it('guard is called after membership is loaded (store pre-populated)', () => {
    authStore.accessToken.set('valid-token');
    membershipStore.setMembership('team-alpha', 'OWNER');

    const guard = roleGuard(['OWNER']);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as never, makeSegments(['teams', 'team-alpha', 'members']))
    );
    expect(result).toBe(true);
  });
});
