import { inject } from '@angular/core';
import { type HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError, switchMap } from 'rxjs/operators';
import { EMPTY, throwError } from 'rxjs';
import { RefreshCoordinator } from '../refresh-coordinator.service';
import { AuthStore } from '../auth.store';

function isAuthEndpoint(url: string): boolean {
  return url.includes('/api/v1/auth/');
}

export const refreshInterceptor: HttpInterceptorFn = (req, next) => {
  const coordinator = inject(RefreshCoordinator);
  const auth = inject(AuthStore);

  return next(req).pipe(
    catchError((err: unknown) => {
      if (!(err instanceof HttpErrorResponse) || err.status !== 401 || isAuthEndpoint(req.url)) {
        return throwError(() => err);
      }

      return coordinator.refresh().pipe(
        switchMap((newToken: string) =>
          next(req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } })),
        ),
        catchError(() => {
          auth.clearAuth();
          return EMPTY;
        }),
      );
    }),
  );
};
