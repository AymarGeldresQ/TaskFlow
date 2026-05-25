import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService', () => {
  let service: TokenStorageService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(TokenStorageService);
  });

  it('returns null when refresh token is not set', () => {
    expect(service.getRefreshToken()).toBeNull();
  });

  it('stores and retrieves refresh token', () => {
    service.setRefreshToken('my-refresh-token');
    expect(service.getRefreshToken()).toBe('my-refresh-token');
  });

  it('clears the refresh token', () => {
    service.setRefreshToken('my-refresh-token');
    service.clearRefreshToken();
    expect(service.getRefreshToken()).toBeNull();
  });
});
