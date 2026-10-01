import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page-container not-found">
      <h1>404</h1>
      <p>We couldn't find the page you were looking for.</p>
      <a routerLink="/" class="btn-primary">Back to Home</a>
      <a routerLink="/products" class="secondary-link">Continue Shopping</a>
    </section>
  `,
  styles: [`
    .not-found { text-align: center; padding: 96px 24px; }
    .not-found h1 { font-size: 4rem; color: var(--color-primary); }
    .not-found a.btn-primary { display: inline-block; text-decoration: none; margin-top: 16px; }
    .secondary-link { display: block; margin-top: 12px; font-size: 0.875rem; }
  `],
})
export class NotFoundComponent {}
