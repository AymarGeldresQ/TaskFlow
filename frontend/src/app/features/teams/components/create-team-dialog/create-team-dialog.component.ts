import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { type CreateTeamRequest } from '../../../../shared/models';

@Component({
  selector: 'tf-create-team-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Create Team</h2>
    <mat-dialog-content>
      <form [formGroup]="form" id="create-team-form" (ngSubmit)="confirm()">
        <mat-form-field appearance="outline">
          <mat-label>Team Name</mat-label>
          <input matInput formControlName="name" aria-label="Team Name" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Description (optional)</mat-label>
          <textarea matInput formControlName="description" aria-label="Description"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" form="create-team-form" type="submit" [disabled]="form.invalid">
        Create
      </button>
    </mat-dialog-actions>
  `,
  styles: [`mat-form-field { width: 100%; display: block; margin-bottom: 8px; }`],
})
export class CreateTeamDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<CreateTeamDialogComponent>);

  protected readonly form = new FormGroup({
    name: new FormControl('', { validators: [Validators.required, Validators.minLength(2)], nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
  });

  protected confirm(): void {
    if (this.form.invalid) return;
    const { name, description } = this.form.getRawValue();
    const req: CreateTeamRequest = { name, ...(description ? { description } : {}) };
    this.dialogRef.close(req);
  }
}
