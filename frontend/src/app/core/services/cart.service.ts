import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Cart } from '../models/cart.model';

/**
 * Unlike a purely client-side cart, this one is server-authoritative: every
 * mutation hits the backend (which owns pricing, stock checks, and
 * persistence) and the response replaces local state. The signal exists so
 * components can read cart state synchronously without every one of them
 * re-fetching — not as a cache that could drift from the server.
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/cart`;

  private readonly _cart = signal<Cart | null>(null);
  readonly cart = this._cart.asReadonly();
  readonly itemCount = computed(() => this._cart()?.itemCount ?? 0);

  load(): Observable<Cart> {
    return this.http.get<Cart>(this.baseUrl).pipe(tap((c) => this._cart.set(c)));
  }

  addItem(productVariantId: number, quantity: number): Observable<Cart> {
    return this.http
      .post<Cart>(`${this.baseUrl}/items`, { productVariantId, quantity })
      .pipe(tap((c) => this._cart.set(c)));
  }

  updateQuantity(itemId: number, quantity: number): Observable<Cart> {
    return this.http
      .patch<Cart>(`${this.baseUrl}/items/${itemId}`, { quantity })
      .pipe(tap((c) => this._cart.set(c)));
  }

  removeItem(itemId: number): Observable<Cart> {
    return this.http
      .delete<Cart>(`${this.baseUrl}/items/${itemId}`)
      .pipe(tap((c) => this._cart.set(c)));
  }

  clear(): Observable<void> {
    return this.http.delete<void>(this.baseUrl).pipe(tap(() => this._cart.set(null)));
  }
}
