import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthStore } from '../../../core/auth/auth.store';

@Component({
  selector: 'tf-login-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCardModule,
  ],
  template: `
    <mat-card class="auth-card">
      <mat-card-header>
        <mat-card-title>Sign In</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input
              matInput
              type="email"
              formControlName="email"
              id="email"
              aria-label="Email"
            />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Password</mat-label>
            <input
              matInput
              type="password"
              formControlName="password"
              id="password"
              aria-label="Password"
            />
          </mat-form-field>

          @if (errorMessage()) {
            <div role="alert" class="error-message">{{ errorMessage() }}</div>
          }

          <button
            mat-flat-button
            color="primary"
            type="submit"
            [disabled]="loading()"
          >
            Sign In
          </button>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .auth-card { max-width: 400px; margin: 80px auto; }
    mat-form-field { width: 100%; display: block; margin-bottom: 8px; }
    button { width: 100%; margin-top: 16px; }
    .error-message { color: var(--mat-sys-error, red); margin-bottom: 8px; }
  `],
})
export class LoginPageComponent {
  private readonly authStore = inject(AuthStore);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly loading = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly form = new FormGroup({
    email: new FormControl('', {
      validators: [Validators.required, Validators.email],
      nonNullable: true,
    }),
    password: new FormControl('', {
      validators: [Validators.required, Validators.minLength(8)],
      nonNullable: true,
    }),
  });

  protected async submit(): Promise<void> {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    try {
      const { email, password } = this.form.getRawValue();
      await this.authStore.login(email, password);

      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/teams';
      await this.router.navigateByUrl(returnUrl);
    } catch (err: unknown) {
      const errObj = err as Record<string, unknown>;
      const msg = err && typeof err === 'object' && 'message' in err
        ? String(errObj['message'])
        : 'Login failed. Please try again.';
      this.errorMessage.set(msg);
    } finally {
      this.loading.set(false);
    }
  }
}
