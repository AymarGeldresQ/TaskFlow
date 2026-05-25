import { Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { type AddMemberRequest, type TeamRole } from '../../../../shared/models';

@Component({
  selector: 'tf-invite-member-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>Invite Member</h2>
    <mat-dialog-content>
      <form [formGroup]="form" id="invite-member-form" (ngSubmit)="confirm()">
        <mat-form-field appearance="outline">
          <mat-label>User ID</mat-label>
          <input matInput formControlName="userId" aria-label="User ID" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Role</mat-label>
          <mat-select formControlName="role" aria-label="Role">
            @for (role of roles; track role) {
              <mat-option [value]="role">{{ role }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" form="invite-member-form" type="submit" [disabled]="form.invalid">
        Invite
      </button>
    </mat-dialog-actions>
  `,
  styles: [`mat-form-field { width: 100%; display: block; margin-bottom: 8px; }`],
})
export class InviteMemberDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<InviteMemberDialogComponent>);

  protected readonly roles: TeamRole[] = ['ADMIN', 'MEMBER', 'VIEWER'];

  protected readonly form = new FormGroup({
    userId: new FormControl('', { validators: [Validators.required], nonNullable: true }),
    role: new FormControl<TeamRole>('MEMBER', { validators: [Validators.required], nonNullable: true }),
  });

  protected confirm(): void {
    if (this.form.invalid) return;
    const req: AddMemberRequest = this.form.getRawValue();
    this.dialogRef.close(req);
  }
}
