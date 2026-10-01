import { ChangeDetectionStrategy, Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { ReviewService } from '../../../core/services/review.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/services/toast.service';
import { ProductDetail, ProductSummary, ProductVariant } from '../../../core/models/product.model';
import { Review } from '../../../core/models/misc.model';
import { RatingComponent } from '../../../shared/components/rating/rating.component';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';
import { LoadingSkeletonComponent } from '../../../shared/components/loading-skeleton/loading-skeleton.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, RatingComponent, ProductCardComponent, LoadingSkeletonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.scss',
})
export class ProductDetailComponent {
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly wishlistService = inject(WishlistService);
  private readonly reviewService = inject(ReviewService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly product = signal<ProductDetail | null>(null);
  readonly relatedProducts = signal<ProductSummary[]>([]);
  readonly reviews = signal<Review[]>([]);
  readonly isLoading = signal(true);
  readonly activeImageIndex = signal(0);
  readonly selectedSize = signal<string | null>(null);
  readonly selectedColor = signal<string | null>(null);
  readonly quantity = signal(1);
  readonly isAddingToCart = signal(false);
  readonly isReviewFormOpen = signal(false);

  readonly reviewForm = this.fb.nonNullable.group({
    orderItemId: [0, Validators.required],
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    title: [''],
    body: [''],
  });

  readonly selectedVariant = computed<ProductVariant | undefined>(() => {
    const p = this.product();
    if (!p) return undefined;
    return p.variants.find(
      (v) => (this.selectedSize() ? v.size === this.selectedSize() : true)
          && (this.selectedColor() ? v.color === this.selectedColor() : true)
    );
  });

  constructor() {
    const route = inject(ActivatedRoute);
    route.paramMap.pipe(
      switchMap((params) => {
        this.isLoading.set(true);
        const slug = params.get('slug')!;
        return this.productService.getBySlug(slug).pipe(catchError(() => of(null)));
      })
    ).subscribe((product) => {
      this.isLoading.set(false);
      this.product.set(product);
      if (product) {
        this.selectedSize.set(product.availableSizes[0] ?? null);
        this.selectedColor.set(product.availableColors[0] ?? null);
        this.productService.getRelated(product.id).pipe(catchError(() => of([]))).subscribe((r) => this.relatedProducts.set(r));
        this.reviewService.listForProduct(product.id).pipe(catchError(() => of(null))).subscribe((res) => {
          if (res) this.reviews.set(res.items);
        });
      }
    });
  }

  selectSize(size: string): void { this.selectedSize.set(size); }
  selectColor(color: string): void { this.selectedColor.set(color); }

  incrementQuantity(): void { this.quantity.update((q) => Math.min(q + 1, 20)); }
  decrementQuantity(): void { this.quantity.update((q) => Math.max(q - 1, 1)); }

  addToCart(): void {
    if (!this.auth.isAuthenticated()) { this.router.navigate(['/auth/login']); return; }
    const variant = this.selectedVariant();
    if (!variant) { this.toast.show('Please select a size and color.', 'error'); return; }
    if (!variant.inStock) { this.toast.show('This combination is out of stock.', 'error'); return; }

    this.isAddingToCart.set(true);
    this.cartService.addItem(variant.id, this.quantity()).subscribe({
      next: () => { this.isAddingToCart.set(false); this.toast.show('Added to cart.', 'success'); },
      error: () => this.isAddingToCart.set(false),
    });
  }

  toggleWishlist(): void {
    if (!this.auth.isAuthenticated()) { this.router.navigate(['/auth/login']); return; }
    const p = this.product();
    if (!p) return;
    const action = this.wishlistService.isWishlisted(p.id)
      ? this.wishlistService.remove(p.id)
      : this.wishlistService.add(p.id);
    action.subscribe();
  }

  isWishlisted(): boolean {
    const p = this.product();
    return p ? this.wishlistService.isWishlisted(p.id) : false;
  }

  submitReview(): void {
    if (this.reviewForm.invalid) return;
    this.reviewService.create(this.reviewForm.getRawValue()).subscribe({
      next: (review) => {
        this.reviews.update((list) => [review, ...list]);
        this.isReviewFormOpen.set(false);
        this.toast.show('Thanks for your review!', 'success');
      },
    });
  }
}
