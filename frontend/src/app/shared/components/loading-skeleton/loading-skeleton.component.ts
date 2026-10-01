import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loading-skeleton',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="skeleton-wrap" [attr.aria-hidden]="true">
      @if (variant === 'product-grid') {
        <div class="grid">
          @for (i of range(count); track i) {
            <div class="card-sk">
              <div class="img-sk"></div>
              <div class="line-sk w-70"></div>
              <div class="line-sk w-40"></div>
            </div>
          }
        </div>
      } @else if (variant === 'text') {
        @for (i of range(count); track i) {
          <div class="line-sk" [class.w-60]="i === count - 1"></div>
        }
      } @else {
        <div class="block-sk"></div>
      }
    </div>
  `,
  styles: [`
    .skeleton-wrap * { animation: pulse 1.4s ease-in-out infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 24px; }
    .img-sk { aspect-ratio: 3/4; background: var(--color-border); border-radius: var(--radius-md); }
    .line-sk { height: 12px; background: var(--color-border); border-radius: 4px; margin-top: 8px; width: 100%; }
    .w-70 { width: 70%; } .w-40 { width: 40%; } .w-60 { width: 60%; }
    .block-sk { width: 100%; height: 100%; background: var(--color-border); border-radius: var(--radius-md); }
  `],
})
export class LoadingSkeletonComponent {
  @Input() variant: 'product-grid' | 'text' | 'block' = 'block';
  @Input() count = 8;

  range(n: number): number[] { return Array.from({ length: n }, (_, i) => i); }
}
