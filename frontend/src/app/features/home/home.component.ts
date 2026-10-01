import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { ProductService } from '../../core/services/product.service';
import { CategoryService } from '../../core/services/category.service';
import { WishlistService } from '../../core/services/wishlist.service';
import { ProductSummary } from '../../core/models/product.model';
import { Category } from '../../core/models/category.model';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { LoadingSkeletonComponent } from '../../shared/components/loading-skeleton/loading-skeleton.component';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent, LoadingSkeletonComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly wishlistService = inject(WishlistService);
  private readonly auth = inject(AuthService);

  readonly bestSellers = signal<ProductSummary[]>([]);
  readonly newArrivals = signal<ProductSummary[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly isLoadingBestSellers = signal(true);
  readonly isLoadingNewArrivals = signal(true);

  constructor() {
    this.productService.getBestSellers(8).pipe(
      catchError(() => of([])),
      finalize(() => this.isLoadingBestSellers.set(false))
    ).subscribe((p) => this.bestSellers.set(p));

    this.productService.getNewArrivals(8).pipe(
      catchError(() => of([])),
      finalize(() => this.isLoadingNewArrivals.set(false))
    ).subscribe((p) => this.newArrivals.set(p));

    this.categoryService.list().pipe(catchError(() => of([]))).subscribe((c) => this.categories.set(c));

    if (this.auth.isAuthenticated()) {
      this.wishlistService.load().subscribe();
    }
  }

  onWishlistToggle(product: ProductSummary): void {
    if (!this.auth.isAuthenticated()) return;
    const action = this.wishlistService.isWishlisted(product.id)
      ? this.wishlistService.remove(product.id)
      : this.wishlistService.add(product.id);
    action.subscribe();
  }
}
