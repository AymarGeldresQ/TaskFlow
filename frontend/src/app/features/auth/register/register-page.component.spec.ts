import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { Component } from '@angular/core';
import { RegisterPageComponent } from './register-page.component';
import { AuthStore } from '../../../core/auth/auth.store';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

@Component({ template: '', standalone: true })
class DummyTeamsComponent {}

describe('RegisterPageComponent', () => {
  let fixture: ComponentFixture<RegisterPageComponent>;
  let authStore: AuthStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RegisterPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'teams', component: DummyTeamsComponent }]),
        provideAnimationsAsync(), // eslint-disable-line @typescript-eslint/no-deprecated
      ],
    }).compileComponents();

    authStore = TestBed.inject(AuthStore);
    fixture = TestBed.createComponent(RegisterPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  function getNativeEl(): HTMLElement {
    return fixture.nativeElement;
  }

  function fillForm(name: string, email: string, password: string): void {
    const nameInput: HTMLInputElement = getNativeEl().querySelector('input[aria-label="Name"]')!;
    const emailInput: HTMLInputElement = getNativeEl().querySelector('input[aria-label="Email"]')!;
    const passwordInput: HTMLInputElement = getNativeEl().querySelector('input[aria-label="Password"]')!;
    nameInput.value = name;
    nameInput.dispatchEvent(new Event('input'));
    emailInput.value = email;
    emailInput.dispatchEvent(new Event('input'));
    passwordInput.value = password;
    passwordInput.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('renders name, email, password inputs and submit button', () => {
    const el = getNativeEl();
    expect(el.querySelector('input[aria-label="Name"]')).toBeTruthy();
    expect(el.querySelector('input[aria-label="Email"]')).toBeTruthy();
    expect(el.querySelector('input[aria-label="Password"]')).toBeTruthy();
    expect(el.querySelector('button[type="submit"]')).toBeTruthy();
  });

  it('submit button label is "Create Account"', () => {
    const btn: HTMLButtonElement = getNativeEl().querySelector('button[type="submit"]')!;
    expect(btn.textContent?.trim()).toBe('Create Account');
  });

  it('calls AuthStore.register with email, password, name on valid submit', async () => {
    const registerSpy = vi.spyOn(authStore, 'register').mockResolvedValue();
    fillForm('Alice Smith', 'alice@example.com', 'password123');

    const form: HTMLFormElement = getNativeEl().querySelector('form')!;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(registerSpy).toHaveBeenCalledWith('alice@example.com', 'password123', 'Alice Smith');
  });

  it('does not call AuthStore.register when name is empty', async () => {
    const registerSpy = vi.spyOn(authStore, 'register').mockResolvedValue();
    fillForm('', 'alice@example.com', 'password123');

    const form: HTMLFormElement = getNativeEl().querySelector('form')!;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(registerSpy).not.toHaveBeenCalled();
  });

  it('shows error message when registration fails', async () => {
    vi.spyOn(authStore, 'register').mockRejectedValue({ message: 'Email already taken' });
    fillForm('Bob Jones', 'taken@example.com', 'password123');

    const form: HTMLFormElement = getNativeEl().querySelector('form')!;
    form.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const alert: HTMLElement | null = getNativeEl().querySelector('[role="alert"]');
    expect(alert).toBeTruthy();
    expect(alert?.textContent?.trim()).toBe('Email already taken');
  });
});
