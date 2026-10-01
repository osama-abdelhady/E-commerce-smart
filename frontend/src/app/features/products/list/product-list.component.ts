import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { WishlistService } from '../../../core/services/wishlist.service';
import { AuthService } from '../../../core/services/auth.service';
import { ProductFilter, ProductSortOption, ProductSummary } from '../../../core/models/product.model';
import { Brand } from '../../../core/models/category.model';
import { ProductCardComponent } from '../../../shared/components/product-card/product-card.component';
import { LoadingSkeletonComponent } from '../../../shared/components/loading-skeleton/loading-skeleton.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ProductCardComponent, LoadingSkeletonComponent, EmptyStateComponent, PaginationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly wishlistService = inject(WishlistService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly products = signal<ProductSummary[]>([]);
  readonly brands = signal<Brand[]>([]);
  readonly isLoading = signal(true);
  readonly totalItems = signal(0);
  readonly totalPages = signal(0);
  readonly isFilterOpen = signal(false);

  readonly filter = signal<ProductFilter>({ page: 0, pageSize: 24, sortBy: 'NEWEST' });

  constructor() {
    this.categoryService.getBrands().pipe(catchError(() => of([]))).subscribe((b) => this.brands.set(b));

    this.route.queryParamMap.subscribe((params) => {
      const next: ProductFilter = {
        categorySlug: params.get('categorySlug') ?? undefined,
        brand: params.getAll('brand').length ? params.getAll('brand') : undefined,
        search: params.get('search') ?? undefined,
        minPrice: params.get('minPrice') ? Number(params.get('minPrice')) : undefined,
        maxPrice: params.get('maxPrice') ? Number(params.get('maxPrice')) : undefined,
        inStockOnly: params.get('inStockOnly') === 'true' ? true : undefined,
        sortBy: (params.get('sortBy') as ProductSortOption) ?? 'NEWEST',
        page: params.get('page') ? Number(params.get('page')) : 0,
        pageSize: 24,
      };
      this.filter.set(next);
      this.fetch();
    });
  }

  private fetch(): void {
    this.isLoading.set(true);
    this.productService.list(this.filter()).pipe(catchError(() => of(null))).subscribe((res) => {
      this.isLoading.set(false);
      if (!res) return;
      this.products.set(res.items);
      this.totalItems.set(res.totalItems);
      this.totalPages.set(res.totalPages);
    });
    if (this.auth.isAuthenticated()) this.wishlistService.load().subscribe();
  }

  updateQueryParams(patch: Record<string, string | number | boolean | undefined | null>): void {
    const queryParams = { ...patch, page: patch['page'] !== undefined ? patch['page'] : 0 };
    this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge' });
  }

  onSortChange(sortBy: string): void {
    this.updateQueryParams({ sortBy });
  }

  onBrandToggle(slug: string, checked: boolean): void {
    const current = this.filter().brand ?? [];
    const next = checked ? [...current, slug] : current.filter((b) => b !== slug);
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { brand: next.length ? next : null, page: 0 },
      queryParamsHandling: 'merge',
    });
  }

  onPriceFilter(minPrice: string, maxPrice: string): void {
    this.updateQueryParams({
      minPrice: minPrice || null,
      maxPrice: maxPrice || null,
    });
  }

  onPageChange(page: number): void {
    this.updateQueryParams({ page });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  onWishlistToggle(product: ProductSummary): void {
    if (!this.auth.isAuthenticated()) { this.router.navigate(['/auth/login']); return; }
    const action = this.wishlistService.isWishlisted(product.id)
      ? this.wishlistService.remove(product.id)
      : this.wishlistService.add(product.id);
    action.subscribe();
  }

  isBrandChecked(slug: string): boolean {
    return (this.filter().brand ?? []).includes(slug);
  }
}
