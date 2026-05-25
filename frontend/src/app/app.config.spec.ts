import { describe, it, expect } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { appConfig } from './app.config';

describe('appConfig', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers],
    });
  });

  it('provides HttpClient', () => {
    const client = TestBed.inject(HttpClient);
    expect(client).toBeTruthy();
  });

  it('provides Router', () => {
    const router = TestBed.inject(Router);
    expect(router).toBeTruthy();
  });
});
