import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/misc.model';
import { CheckoutRequest, Order } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/orders`;

  checkout(request: CheckoutRequest): Observable<Order> {
    return this.http.post<Order>(this.baseUrl, request);
  }

  list(page = 0, pageSize = 10): Observable<PageResponse<Order>> {
    return this.http.get<PageResponse<Order>>(this.baseUrl, { params: { page, pageSize } });
  }

  getById(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.baseUrl}/${id}`);
  }

  cancel(id: number, reason?: string): Observable<Order> {
    return this.http.post<Order>(`${this.baseUrl}/${id}/cancel`, {}, { params: reason ? { reason } : {} });
  }

  /** Generates a fresh client-side idempotency key for a new checkout attempt. */
  newIdempotencyKey(): string {
    return crypto.randomUUID();
  }
}
