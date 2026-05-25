import { Injectable } from '@angular/core';

const REFRESH_KEY = 'tf.refresh';

@Injectable({ providedIn: 'root' })
export class TokenStorageService {
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_KEY);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_KEY, token);
  }

  clearRefreshToken(): void {
    localStorage.removeItem(REFRESH_KEY);
  }
}
