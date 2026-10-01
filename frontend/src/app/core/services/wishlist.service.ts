import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Wishlist } from '../models/misc.model';

@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/wishlist`;

  private readonly _wishlist = signal<Wishlist | null>(null);
  readonly wishlist = this._wishlist.asReadonly();
  readonly count = computed(() => this._wishlist()?.items.length ?? 0);

  load(): Observable<Wishlist> {
    return this.http.get<Wishlist>(this.baseUrl).pipe(tap((w) => this._wishlist.set(w)));
  }

  add(productId: number): Observable<Wishlist> {
    return this.http.post<Wishlist>(`${this.baseUrl}/items/${productId}`, {}).pipe(tap((w) => this._wishlist.set(w)));
  }

  remove(productId: number): Observable<Wishlist> {
    return this.http.delete<Wishlist>(`${this.baseUrl}/items/${productId}`).pipe(tap((w) => this._wishlist.set(w)));
  }

  moveToCart(productId: number, variantId?: number, quantity = 1): Observable<Wishlist> {
    const params: Record<string, string> = { quantity: String(quantity) };
    if (variantId) params['variantId'] = String(variantId);
    return this.http
      .post<Wishlist>(`${this.baseUrl}/items/${productId}/move-to-cart`, {}, { params })
      .pipe(tap((w) => this._wishlist.set(w)));
  }

  isWishlisted(productId: number): boolean {
    return this._wishlist()?.items.some((i) => i.productId === productId) ?? false;
  }
}
