import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/misc.model';
import { Brand, Category } from '../models/category.model';
import { ProductSortOption, ProductSummary } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/categories`;

  list(): Observable<Category[]> {
    return this.http.get<Category[]>(this.baseUrl);
  }

  getBySlug(slug: string): Observable<Category> {
    return this.http.get<Category>(`${this.baseUrl}/${slug}`);
  }

  getProducts(
    slug: string,
    opts: { brand?: string[]; sortBy?: ProductSortOption; page?: number; pageSize?: number } = {}
  ): Observable<PageResponse<ProductSummary>> {
    const params: Record<string, string | string[]> = {};
    if (opts.brand?.length) params['brand'] = opts.brand;
    if (opts.sortBy) params['sortBy'] = opts.sortBy;
    if (opts.page !== undefined) params['page'] = String(opts.page);
    if (opts.pageSize !== undefined) params['pageSize'] = String(opts.pageSize);
    return this.http.get<PageResponse<ProductSummary>>(`${this.baseUrl}/${slug}/products`, { params });
  }

  getBrands(): Observable<Brand[]> {
    return this.http.get<Brand[]>(`${this.baseUrl}/brands/all`);
  }
}
