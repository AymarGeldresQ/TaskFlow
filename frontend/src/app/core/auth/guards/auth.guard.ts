import { inject } from '@angular/core';
import { type CanMatchFn, Router } from '@angular/router';
import { AuthStore } from '../auth.store';

export const authGuard: CanMatchFn = () => {
  const auth = inject(AuthStore);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  const returnUrl = encodeURIComponent(location.pathname + location.search);
  return router.parseUrl(`/login?returnUrl=${returnUrl}`);
};
