import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LabelsStore } from '../labels.store';
import { TeamMembershipStore } from '../../../core/auth/team-membership.store';

@Component({
  selector: 'tf-label-list',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="label-list">
      <h3>Labels</h3>

      @if (store.loading()) {
        <mat-spinner diameter="32" />
      } @else {
        <mat-chip-set aria-label="Project labels">
          @for (label of store.labels(); track label.id) {
            <mat-chip [style.background-color]="label.color">
              {{ label.name }}
              @if (canManage()) {
                <button
                  matChipRemove
                  aria-label="Delete label"
                  (click)="deleteLabel(label.id)"
                >
                  <mat-icon>cancel</mat-icon>
                </button>
              }
            </mat-chip>
          }
        </mat-chip-set>
      }

      @if (canManage()) {
        <form [formGroup]="form" (ngSubmit)="addLabel()" class="add-label-form">
          <mat-form-field appearance="outline">
            <mat-label>Label name</mat-label>
            <input matInput formControlName="name" aria-label="Label name" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Color (#RRGGBB)</mat-label>
            <input matInput formControlName="color" aria-label="Label color" placeholder="#FF0000" />
          </mat-form-field>
          <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid">
            Add Label
          </button>
        </form>
      }
    </div>
  `,
  styles: [`
    .label-list { padding: 16px; }
    .add-label-form { display: flex; gap: 8px; align-items: flex-start; flex-wrap: wrap; margin-top: 16px; }
    mat-chip-set { margin-bottom: 16px; display: block; }
  `],
})
export class LabelListComponent implements OnInit {
  protected readonly store = inject(LabelsStore);
  private readonly membershipStore = inject(TeamMembershipStore);
  private readonly route = inject(ActivatedRoute);

  protected readonly form = new FormGroup({
    name: new FormControl('', { validators: [Validators.required], nonNullable: true }),
    color: new FormControl('', {
      validators: [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)],
      nonNullable: true,
    }),
  });

  ngOnInit(): void {
    const projectId = this.projectId();
    if (projectId) {
      void this.store.loadLabels(projectId);
    }
  }

  protected canManage(): boolean {
    const teamId = this.route.snapshot.paramMap.get('teamId');
    if (!teamId) return false;
    const role = this.membershipStore.roleFor(teamId);
    return role === 'OWNER' || role === 'ADMIN';
  }

  protected addLabel(): void {
    if (this.form.invalid) return;
    const projectId = this.projectId();
    if (!projectId) return;
    const { name, color } = this.form.getRawValue();
    void this.store.createLabel(projectId, { name, color });
    this.form.reset();
  }

  protected deleteLabel(labelId: string): void {
    void this.store.deleteLabel(labelId);
  }

  private projectId(): string | null {
    return this.route.snapshot.paramMap.get('projectId');
  }
}
