import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductSummary } from '../../../core/models/product.model';
import { WishlistService } from '../../../core/services/wishlist.service';
import { RatingComponent } from '../rating/rating.component';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, RouterLink, RatingComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="product-card">
      <a [routerLink]="['/products', product.slug]" class="image-link">
        @if (product.primaryImageUrl) {
          <img [src]="product.primaryImageUrl" [alt]="product.name" loading="lazy" />
        } @else {
          <div class="image-placeholder"></div>
        }
        <div class="badges">
          @if (product.isNewArrival) { <span class="badge new">New</span> }
          @if (product.discountPrice) { <span class="badge sale">Sale</span> }
          @if (!product.inStock) { <span class="badge out">Out of stock</span> }
        </div>
        <button
          type="button"
          class="wishlist-btn"
          [attr.aria-pressed]="isWishlisted()"
          [attr.aria-label]="isWishlisted() ? 'Remove from wishlist' : 'Add to wishlist'"
          (click)="onToggleWishlist($event)"
        >
          {{ isWishlisted() ? '♥' : '♡' }}
        </button>
      </a>
      <div class="info">
        @if (product.brandName) { <span class="brand">{{ product.brandName }}</span> }
        <h3 class="name"><a [routerLink]="['/products', product.slug]">{{ product.name }}</a></h3>
        <app-rating [value]="product.averageRating" [count]="product.reviewCount"></app-rating>
        <div class="price-row">
          <span class="price">{{ (product.discountPrice ?? product.price) | currency:product.currency }}</span>
          @if (product.discountPrice) {
            <span class="price-original">{{ product.price | currency:product.currency }}</span>
          }
        </div>
      </div>
    </article>
  `,
  styles: [`
    .product-card { display: flex; flex-direction: column; }
    .image-link { position: relative; display: block; aspect-ratio: 3/4; border-radius: var(--radius-md); overflow: hidden; background: var(--color-surface-alt); }
    .image-link img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s ease; }
    .image-link:hover img { transform: scale(1.03); }
    .image-placeholder { width: 100%; height: 100%; background: var(--color-border); }
    .badges { position: absolute; top: 8px; left: 8px; display: flex; flex-direction: column; gap: 4px; }
    .badge { font-size: 0.65rem; font-weight: 700; text-transform: uppercase; padding: 3px 8px; border-radius: var(--radius-sm); color: #fff; }
    .badge.new { background: var(--color-primary); }
    .badge.sale { background: var(--color-accent); }
    .badge.out { background: #6b7280; }
    .wishlist-btn {
      position: absolute; top: 8px; right: 8px; width: 32px; height: 32px; border-radius: 50%;
      border: none; background: rgba(255,255,255,0.9); font-size: 1.1rem; cursor: pointer;
      display: flex; align-items: center; justify-content: center; color: var(--color-danger);
    }
    .info { padding-top: 10px; display: flex; flex-direction: column; gap: 4px; }
    .brand { font-size: 0.7rem; text-transform: uppercase; letter-spacing: 0.04em; color: var(--color-text-muted); }
    .name { font-size: 0.95rem; margin: 0; }
    .name a { color: var(--color-ink); }
    .price-row { display: flex; align-items: baseline; gap: 8px; margin-top: 2px; }
    .price { font-weight: 700; }
    .price-original { font-size: 0.85rem; color: var(--color-text-muted); text-decoration: line-through; }
  `],
})
export class ProductCardComponent {
  @Input({ required: true }) product!: ProductSummary;
  @Output() wishlistToggled = new EventEmitter<ProductSummary>();

  private readonly wishlistService = inject(WishlistService);

  isWishlisted(): boolean {
    return this.wishlistService.isWishlisted(this.product.id);
  }

  onToggleWishlist(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.wishlistToggled.emit(this.product);
  }
}
