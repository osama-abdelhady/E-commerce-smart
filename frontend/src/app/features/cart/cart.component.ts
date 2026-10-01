import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, EmptyStateComponent, LoadingSkeletonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent {
  readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  readonly isLoading = signal(true);
  readonly updatingItemId = signal<number | null>(null);

  constructor() {
    this.cartService.load().subscribe({ next: () => this.isLoading.set(false), error: () => this.isLoading.set(false) });
  }

  updateQuantity(itemId: number, quantity: number): void {
    if (quantity < 1) return;
    this.updatingItemId.set(itemId);
    this.cartService.updateQuantity(itemId, quantity).subscribe({
      complete: () => this.updatingItemId.set(null),
    });
  }

  removeItem(itemId: number): void {
    this.updatingItemId.set(itemId);
    this.cartService.removeItem(itemId).subscribe({
      complete: () => this.updatingItemId.set(null),
    });
  }

  goToCheckout(): void {
    this.router.navigate(['/checkout']);
  }
}
