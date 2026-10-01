import { ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-rating',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="rating" role="img" [attr.aria-label]="'Rated ' + ratingValue() + ' out of 5 stars'">
      @for (star of stars(); track $index) {
        <span class="star" [class.full]="star !== 'empty'" [class.half]="star === 'half'">★</span>
      }
      @if (showCount && count !== undefined) {
        <span class="count">({{ count }})</span>
      }
    </div>
  `,
  styles: [`
    .rating { display: inline-flex; align-items: center; gap: 2px; }
    .star { color: #d1d5db; font-size: 0.95rem; }
    .star.full { color: #d97706; }
    .count { margin-left: 4px; font-size: 0.8rem; color: var(--color-text-muted); }
  `],
})
export class RatingComponent {
  @Input({ required: true }) set value(v: number) { this.ratingValue.set(v); }
  @Input() count?: number;
  @Input() showCount = true;

  protected readonly ratingValue = signal(0);

  readonly stars = computed(() => {
    const v = this.ratingValue();
    return Array.from({ length: 5 }, (_, i) => {
      const diff = v - i;
      if (diff >= 1) return 'full';
      if (diff >= 0.5) return 'half';
      return 'empty';
    });
  });
}
