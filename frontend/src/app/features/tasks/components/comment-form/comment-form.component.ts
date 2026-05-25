import { Component, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'tf-comment-form',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <form class="comment-form" (ngSubmit)="onSubmit()">
      <mat-form-field class="comment-form__field" appearance="outline">
        <mat-label>Add a comment</mat-label>
        <textarea
          matInput
          [(ngModel)]="body"
          name="body"
          rows="3"
          [disabled]="false"
        ></textarea>
      </mat-form-field>
      <button mat-raised-button color="primary" type="submit" [disabled]="!body().trim()">
        Submit
      </button>
    </form>
  `,
  styles: [
    `
      .comment-form {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .comment-form__field {
        width: 100%;
      }
    `,
  ],
})
export class CommentFormComponent {
  readonly submitted = output<string>();

  protected readonly body = signal('');

  protected onSubmit(): void {
    const text = this.body().trim();
    if (!text) return;
    this.submitted.emit(text);
    this.body.set('');
  }
}
