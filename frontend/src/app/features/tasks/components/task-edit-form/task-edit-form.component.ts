import { Component, OnInit, input, output } from '@angular/core';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { type Task, type UpdateTaskRequest, type TaskPriority } from '../../../../shared/models';

type EditForm = FormGroup<{
  title: FormControl<string>;
  description: FormControl<string | null>;
  priority: FormControl<TaskPriority>;
  dueDate: FormControl<string | null>;
}>;

@Component({
  selector: 'tf-task-edit-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <form class="task-edit-form" [formGroup]="form" (ngSubmit)="onSave()">
      <mat-form-field appearance="outline" class="task-edit-form__field">
        <mat-label>Title</mat-label>
        <input matInput formControlName="title" />
      </mat-form-field>

      <mat-form-field appearance="outline" class="task-edit-form__field">
        <mat-label>Description</mat-label>
        <textarea matInput formControlName="description" rows="3"></textarea>
      </mat-form-field>

      <mat-form-field appearance="outline" class="task-edit-form__field">
        <mat-label>Priority</mat-label>
        <mat-select formControlName="priority">
          <mat-option value="LOW">Low</mat-option>
          <mat-option value="MEDIUM">Medium</mat-option>
          <mat-option value="HIGH">High</mat-option>
          <mat-option value="CRITICAL">Critical</mat-option>
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" class="task-edit-form__field">
        <mat-label>Due Date</mat-label>
        <input matInput formControlName="dueDate" type="date" />
      </mat-form-field>

      <div class="task-edit-form__actions">
        <button mat-button type="button" (click)="cancelled.emit()">Cancel</button>
        <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid">
          Save
        </button>
      </div>
    </form>
  `,
  styles: [
    `
      .task-edit-form {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .task-edit-form__field {
        width: 100%;
      }
      .task-edit-form__actions {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
      }
    `,
  ],
})
export class TaskEditFormComponent implements OnInit {
  readonly task = input.required<Task>();
  readonly saved = output<UpdateTaskRequest>();
  readonly cancelled = output();

  protected form!: EditForm;

  ngOnInit(): void {
    const t = this.task();
    this.form = new FormGroup({
      title: new FormControl<string>(t.title, { nonNullable: true, validators: [Validators.required] }),
      description: new FormControl<string | null>(t.description),
      priority: new FormControl<TaskPriority>(t.priority, { nonNullable: true }),
      dueDate: new FormControl<string | null>(t.dueDate),
    });
  }

  protected onSave(): void {
    if (this.form.invalid) return;
    const { title, description, priority, dueDate } = this.form.getRawValue();
    this.saved.emit({
      title,
      description: description ?? undefined,
      priority,
      dueDate: dueDate ?? undefined,
    });
  }
}
