import { Injectable, inject } from '@angular/core';
import { type Observable, ReplaySubject, throwError } from 'rxjs';
import { AuthApiClient } from './auth-api-client.service';
import { AuthStore } from './auth.store';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class RefreshCoordinator {
  private readonly api = inject(AuthApiClient);
  private readonly auth = inject(AuthStore);
  private readonly storage = inject(TokenStorageService);

  private refresh$: Observable<string> | null = null;

  refresh(): Observable<string> {
    if (this.refresh$ !== null) {
      return this.refresh$;
    }

    const refreshToken = this.storage.getRefreshToken();
    if (refreshToken === null) {
      return throwError(() => new Error('no_refresh_token'));
    }

    const subject = new ReplaySubject<string>(1);
    this.refresh$ = subject.asObservable();

    this.api.refresh({ refreshToken }).subscribe({
      next: (res) => {
        this.auth.setTokens(res.accessToken, res.refreshToken);
        subject.next(res.accessToken);
        subject.complete();
        this.refresh$ = null;
      },
      error: (err: unknown) => {
        this.auth.logout();
        subject.error(err);
        this.refresh$ = null;
      },
    });

    return this.refresh$;
  }
}
