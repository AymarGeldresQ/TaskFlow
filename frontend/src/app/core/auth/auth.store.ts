import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { type User, type LoginRequest, type RegisterRequest } from '../../shared/models';
import { AuthApiClient } from './auth-api-client.service';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly api = inject(AuthApiClient);
  private readonly storage = inject(TokenStorageService);
  private readonly router = inject(Router);

  readonly accessToken = signal<string | null>(null);
  readonly currentUser = signal<User | null>(null);
  readonly isAuthenticated = computed(() => this.accessToken() !== null);

  async login(email: string, password: string): Promise<void> {
    const res = await firstValueFrom(this.api.login({ email, password } satisfies LoginRequest));
    this.accessToken.set(res.accessToken);
    this.storage.setRefreshToken(res.refreshToken);
    this.currentUser.set(res.user);
  }

  async register(email: string, password: string, name: string): Promise<void> {
    const res = await firstValueFrom(this.api.register({ email, password, name } satisfies RegisterRequest));
    this.accessToken.set(res.accessToken);
    this.storage.setRefreshToken(res.refreshToken);
    this.currentUser.set(res.user);
  }

  setTokens(access: string, refresh: string): void {
    this.accessToken.set(access);
    this.storage.setRefreshToken(refresh);
  }

  getAccessToken(): string | null {
    return this.accessToken();
  }

  clearAuth(): void {
    this.accessToken.set(null);
    this.currentUser.set(null);
    this.storage.clearRefreshToken();
  }

  logout(): void {
    this.clearAuth();
    void this.router.navigateByUrl('/login');
  }
}
