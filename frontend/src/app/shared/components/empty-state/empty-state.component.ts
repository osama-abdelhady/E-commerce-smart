import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty-state">
      <h3>{{ title }}</h3>
      <p>{{ description }}</p>
      @if (actionLabel && actionLink) {
        <a [routerLink]="actionLink" class="btn-primary">{{ actionLabel }}</a>
      }
    </div>
  `,
  styles: [`
    .empty-state { text-align: center; padding: 64px 24px; color: var(--color-text-muted); }
    .empty-state h3 { color: var(--color-ink); margin-bottom: 8px; }
    .empty-state a { display: inline-block; margin-top: 16px; text-decoration: none; }
  `],
})
export class EmptyStateComponent {
  @Input() title = 'Nothing here yet';
  @Input() description = '';
  @Input() actionLabel?: string;
  @Input() actionLink?: string;
}
