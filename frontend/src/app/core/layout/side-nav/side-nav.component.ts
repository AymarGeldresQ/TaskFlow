import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { type Project } from '../../../shared/models';

@Component({
  selector: 'tf-side-nav',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, MatListModule],
  template: `
    <mat-nav-list>
      @for (project of projects(); track project.id) {
        <a
          mat-list-item
          [routerLink]="['/teams', project.teamId, 'projects', project.id, 'board']"
          routerLinkActive="active"
        >
          {{ project.name }}
        </a>
      }
    </mat-nav-list>
  `,
  styles: [`
    :host { display: block; width: 240px; }
  `],
})
export class SideNavComponent {
  readonly projects = input<readonly Project[]>([]);
}
