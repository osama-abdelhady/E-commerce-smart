import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/misc.model';
import { ProductDetail } from '../models/product.model';

export interface ProductCreatePayload {
  sku: string; slug: string; name: string; description: string;
  price: number; discountPrice: number | null; categoryId: number; brandId: number | null;
  isBestSeller: boolean; isNewArrival: boolean;
  images: { url: string; altText: string; displayOrder: number }[];
  variants: { sku: string; size: string; color: string; colorHex: string; priceOverride: number | null; initialQuantity: number }[];
}

export interface ProductUpdatePayload {
  name: string; description: string; price: number; discountPrice: number | null;
  categoryId: number; brandId: number | null; status: string;
  isBestSeller: boolean; isNewArrival: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminProductService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin/products`;

  list(page = 0, pageSize = 20): Observable<PageResponse<ProductDetail>> {
    return this.http.get<PageResponse<ProductDetail>>(this.baseUrl, { params: { page, pageSize } });
  }

  create(payload: ProductCreatePayload): Observable<ProductDetail> {
    return this.http.post<ProductDetail>(this.baseUrl, payload);
  }

  update(id: number, payload: ProductUpdatePayload): Observable<ProductDetail> {
    return this.http.put<ProductDetail>(`${this.baseUrl}/${id}`, payload);
  }

  deactivate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
