import { type HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiError } from './api-error';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse) {
        const message = extractMessage(err);
        return throwError(() => new ApiError(err.status, message, err.error));
      }
      return throwError(() => err);
    }),
  );
};

function extractMessage(err: HttpErrorResponse): string {
  if (
    err.error !== null &&
    typeof err.error === 'object' &&
    'message' in err.error &&
    typeof (err.error as { message: unknown }).message === 'string'
  ) {
    return (err.error as { message: string }).message;
  }
  return `HTTP ${err.status.toString()}: An unexpected error occurred`;
}
