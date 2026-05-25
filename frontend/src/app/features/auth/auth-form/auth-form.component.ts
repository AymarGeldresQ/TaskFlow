import { Component, input, output } from '@angular/core';
import { type FormGroup } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'tf-auth-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>{{ title() }}</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="form()" (ngSubmit)="formSubmit.emit()">
          <ng-content />
          @if (errorMessage()) {
            <div role="alert" class="error-message">{{ errorMessage() }}</div>
          }
          <button
            mat-flat-button
            color="primary"
            type="submit"
            [disabled]="loading()"
          >
            {{ submitLabel() }}
          </button>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    mat-card { max-width: 400px; margin: 40px auto; }
    mat-form-field { width: 100%; margin-bottom: 8px; }
    button { width: 100%; margin-top: 16px; }
    .error-message { color: var(--mat-sys-error, red); margin-bottom: 8px; }
  `],
})
export class AuthFormComponent {
  readonly title = input.required<string>();
  readonly submitLabel = input.required<string>();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  readonly form = input.required<FormGroup<Record<string, any>>>();
  readonly loading = input(false);
  readonly errorMessage = input<string | null>(null);
  readonly formSubmit = output();
}
