import { inject } from '@angular/core';
import { type CanMatchFn, Router, type UrlSegment, type Route } from '@angular/router';
import { AuthStore } from '../auth.store';
import { TeamMembershipStore } from '../team-membership.store';
import { type TeamRole } from '../../../shared/models';

function extractTeamId(route: Route, segments: UrlSegment[]): string | undefined {
  for (let i = 1; i < segments.length; i++) {
    if (segments[i - 1].path === 'teams') {
      return segments[i].path;
    }
  }
  return route.data?.['teamId'] as string | undefined;
}

export function roleGuard(allowed: readonly TeamRole[]): CanMatchFn {
  return (route: Route, segments: UrlSegment[]) => {
    const auth = inject(AuthStore);
    const membership = inject(TeamMembershipStore);
    const router = inject(Router);

    if (!auth.isAuthenticated()) {
      return router.parseUrl('/login');
    }

    const teamId = extractTeamId(route, segments);

    if (teamId === undefined) {
      return router.parseUrl('/teams');
    }

    const role = membership.roleFor(teamId);
    if (role !== null && allowed.includes(role)) {
      return true;
    }

    return router.parseUrl('/teams');
  };
}
