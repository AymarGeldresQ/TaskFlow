import { inject } from '@angular/core';
import { type HttpInterceptorFn } from '@angular/common/http';
import { AuthStore } from '../auth.store';

function isApiV1(url: string): boolean {
  return url.includes('/api/v1/');
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthStore);
  const token = auth.getAccessToken();

  if (!token || !isApiV1(req.url)) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};
