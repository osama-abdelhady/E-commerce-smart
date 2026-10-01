import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/misc.model';
import { Review, ReviewRequest } from '../models/misc.model';

@Injectable({ providedIn: 'root' })
export class ReviewService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}`;

  listForProduct(productId: number, page = 0, pageSize = 10): Observable<PageResponse<Review>> {
    return this.http.get<PageResponse<Review>>(`${this.baseUrl}/products/${productId}/reviews`, {
      params: { page, pageSize },
    });
  }

  create(request: ReviewRequest): Observable<Review> {
    return this.http.post<Review>(`${this.baseUrl}/reviews`, request);
  }

  update(id: number, request: ReviewRequest): Observable<Review> {
    return this.http.put<Review>(`${this.baseUrl}/reviews/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/reviews/${id}`);
  }
}
