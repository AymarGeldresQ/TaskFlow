import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Component } from '@angular/core';
import { LoginPageComponent } from './login-page.component';
import { AuthStore } from '../../../core/auth/auth.store';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

@Component({ template: '', standalone: true })
class DummyTeamsComponent {}

describe('LoginPageComponent', () => {
  let fixture: ComponentFixture<LoginPageComponent>;
  let authStore: AuthStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'teams', component: DummyTeamsComponent }]),
        provideAnimationsAsync(), // eslint-disable-line @typescript-eslint/no-deprecated
      ],
    }).compileComponents();

    authStore = TestBed.inject(AuthStore);
    fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  function getNativeEl(): HTMLElement {
    return fixture.nativeElement;
  }

  function fillForm(email: string, password: string): void {
    const emailInput: HTMLInputElement = getNativeEl().querySelector('input[aria-label="Email"]')!;
    const passwordInput: HTMLInputElement = getNativeEl().querySelector('input[aria-label="Password"]')!;
    emailInput.value = email;
    emailInput.dispatchEvent(new Event('input'));
    passwordInput.value = password;
    passwordInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('renders email input, password input, and submit button', () => {
    const el = getNativeEl();
    expect(el.querySelector('input[aria-label="Email"]')).toBeTruthy();
    expect(el.querySelector('input[aria-label="Password"]')).toBeTruthy();
    expect(el.querySelector('button[type="submit"]')).toBeTruthy();
  });

  it('submit button label is "Sign In"', () => {
    const btn: HTMLButtonElement = getNativeEl().querySelector('button[type="submit"]')!;
    expect(btn.textContent?.trim()).toBe('Sign In');
  });

  it('calls AuthStore.login with email and password on valid submit', async () => {
    const loginSpy = vi.spyOn(authStore, 'login').mockResolvedValue();
    fillForm('alice@example.com', 'password123');

    const form: HTMLFormElement = getNativeEl().querySelector('form')!;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(loginSpy).toHaveBeenCalledWith('alice@example.com', 'password123');
  });

  it('does not call AuthStore.login when email is empty', async () => {
    const loginSpy = vi.spyOn(authStore, 'login').mockResolvedValue();
    fillForm('', 'password123');

    const form: HTMLFormElement = getNativeEl().querySelector('form')!;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(loginSpy).not.toHaveBeenCalled();
  });

  it('shows error message when login fails', async () => {
    vi.spyOn(authStore, 'login').mockRejectedValue({ message: 'Invalid credentials' });
    fillForm('bad@example.com', 'wrongpassword');

    const form: HTMLFormElement = getNativeEl().querySelector('form')!;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert: HTMLElement | null = getNativeEl().querySelector('[role="alert"]');
    expect(alert).toBeTruthy();
    expect(alert?.textContent?.trim()).toBe('Invalid credentials');
  });
});
