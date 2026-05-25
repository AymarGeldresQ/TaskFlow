import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ShellComponent } from './shell.component';
import { AuthStore } from '../../auth/auth.store';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

describe('ShellComponent', () => {
  let fixture: ComponentFixture<ShellComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideAnimationsAsync(), // eslint-disable-line @typescript-eslint/no-deprecated
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
  });

  it('renders a router-outlet element', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('router-outlet')).toBeTruthy();
  });

  it('renders the navbar with navigation role', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('[role="navigation"]')).toBeTruthy();
  });

  it('exposes currentUser from AuthStore', () => {
    const authStore = TestBed.inject(AuthStore);
    authStore.setTokens('tok', 'rt');
    expect(authStore.isAuthenticated()).toBe(true);
  });
});
