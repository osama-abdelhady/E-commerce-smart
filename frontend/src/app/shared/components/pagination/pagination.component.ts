import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (totalPagesValue() > 1) {
      <nav class="pagination" aria-label="Pagination">
        <button type="button" [disabled]="currentPageValue() === 0" (click)="go(currentPageValue() - 1)">‹ Prev</button>
        @for (p of pageNumbers(); track p) {
          <button
            type="button"
            [class.active]="p === currentPageValue()"
            [attr.aria-current]="p === currentPageValue() ? 'page' : null"
            (click)="go(p)"
          >{{ p + 1 }}</button>
        }
        <button type="button" [disabled]="currentPageValue() >= totalPagesValue() - 1" (click)="go(currentPageValue() + 1)">Next ›</button>
      </nav>
    }
  `,
  styles: [`
    .pagination { display: flex; gap: 4px; justify-content: center; margin-top: 32px; }
    button {
      min-width: 36px; height: 36px; border: 1px solid var(--color-border); background: #fff;
      border-radius: var(--radius-sm); cursor: pointer; font-size: 0.875rem;
    }
    button:disabled { opacity: 0.4; cursor: not-allowed; }
    button.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }
  `],
})
export class PaginationComponent {
  @Input({ required: true }) set currentPage(v: number) { this.currentPageValue.set(v); }
  @Input({ required: true }) set totalPages(v: number) { this.totalPagesValue.set(v); }
  @Output() pageChange = new EventEmitter<number>();

  protected readonly currentPageValue = signal(0);
  protected readonly totalPagesValue = signal(0);

  readonly pageNumbers = computed(() => {
    const total = this.totalPagesValue();
    const current = this.currentPageValue();
    const windowSize = 5;
    let start = Math.max(0, current - Math.floor(windowSize / 2));
    const end = Math.min(total, start + windowSize);
    start = Math.max(0, end - windowSize);
    return Array.from({ length: end - start }, (_, i) => start + i);
  });

  go(page: number): void {
    if (page < 0 || page >= this.totalPagesValue()) return;
    this.pageChange.emit(page);
  }
}
