import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable } from 'rxjs';
import { type AuthResponse, type LoginRequest, type RegisterRequest } from '../../shared/models';

interface RefreshRequest {
  refreshToken: string;
}

@Injectable({ providedIn: 'root' })
export class AuthApiClient {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/auth';

  login(body: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/login`, body);
  }

  register(body: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/register`, body);
  }

  refresh(body: RefreshRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.base}/refresh`, body);
  }

  logout(): Observable<unknown> {
    return this.http.post<unknown>(`${this.base}/logout`, {});
  }
}
