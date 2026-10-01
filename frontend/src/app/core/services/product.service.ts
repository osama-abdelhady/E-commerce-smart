import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/misc.model';
import { ProductDetail, ProductFilter, ProductSummary } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/products`;

  list(filter: ProductFilter): Observable<PageResponse<ProductSummary>> {
    let params = new HttpParams();
    Object.entries(filter).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return;
      if (Array.isArray(value)) {
        value.forEach((v) => (params = params.append(key, String(v))));
      } else {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<PageResponse<ProductSummary>>(this.baseUrl, { params });
  }

  getBySlug(slug: string): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.baseUrl}/slug/${slug}`);
  }

  getById(id: number): Observable<ProductDetail> {
    return this.http.get<ProductDetail>(`${this.baseUrl}/${id}`);
  }

  getRelated(id: number): Observable<ProductSummary[]> {
    return this.http.get<ProductSummary[]>(`${this.baseUrl}/${id}/related`);
  }

  getBestSellers(limit = 8): Observable<ProductSummary[]> {
    return this.http.get<ProductSummary[]>(`${this.baseUrl}/best-sellers`, { params: { limit } });
  }

  getNewArrivals(limit = 8): Observable<ProductSummary[]> {
    return this.http.get<ProductSummary[]>(`${this.baseUrl}/new-arrivals`, { params: { limit } });
  }
}
