import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-coming-soon',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page-container coming-soon">
      <h1>{{ title }}</h1>
      <p>{{ description }}</p>
      <a routerLink="/" class="btn-primary">Back to Home</a>
    </section>
  `,
  styles: [`
    .coming-soon { text-align: center; padding: 96px 24px; }
    .coming-soon a { display: inline-block; margin-top: 16px; text-decoration: none; }
  `],
})
export class ComingSoonComponent {
  @Input() title = 'Coming soon';
  @Input() description = 'This page is scaffolded and wired into routing — full implementation lands in the next build pass.';
}
