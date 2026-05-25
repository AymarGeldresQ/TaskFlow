import { inject } from '@angular/core';
import { type CanMatchFn, Router } from '@angular/router';
import { AuthStore } from '../auth.store';

export const unauthGuard: CanMatchFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  return auth.isAuthenticated() ? router.parseUrl('/teams') : true;
};
