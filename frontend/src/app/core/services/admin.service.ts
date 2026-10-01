import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse } from '../models/misc.model';
import { Order } from '../models/order.model';
import {
  Coupon, CustomerSummary, DashboardSummary, InventoryRow, OrderStatusReport, SalesReport,
} from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  getDashboard(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.baseUrl}/dashboard`);
  }

  getSalesReport(fromDate?: string, toDate?: string): Observable<SalesReport> {
    const params: Record<string, string> = {};
    if (fromDate) params['fromDate'] = fromDate;
    if (toDate) params['toDate'] = toDate;
    return this.http.get<SalesReport>(`${this.baseUrl}/reports/sales`, { params });
  }

  getOrderStatusReport(): Observable<OrderStatusReport> {
    return this.http.get<OrderStatusReport>(`${this.baseUrl}/reports/order-status`);
  }

  listOrders(filters: {
    orderNumber?: string; customerEmail?: string; status?: string; page?: number; pageSize?: number;
  }): Observable<PageResponse<Order>> {
    const params: Record<string, string> = {};
    Object.entries(filters).forEach(([k, v]) => { if (v !== undefined && v !== '') params[k] = String(v); });
    return this.http.get<PageResponse<Order>>(`${this.baseUrl}/orders`, { params });
  }

  updateOrderStatus(orderId: number, status: string, note?: string): Observable<Order> {
    return this.http.patch<Order>(`${environment.apiBaseUrl}/orders/${orderId}/status`, { status, note });
  }

  listCustomers(search?: string, page = 0, pageSize = 20): Observable<PageResponse<CustomerSummary>> {
    const params: Record<string, string> = { page: String(page), pageSize: String(pageSize) };
    if (search) params['search'] = search;
    return this.http.get<PageResponse<CustomerSummary>>(`${this.baseUrl}/customers`, { params });
  }

  activateCustomer(id: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/customers/${id}/activate`, {});
  }

  deactivateCustomer(id: number): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/customers/${id}/deactivate`, {});
  }

  listInventory(): Observable<InventoryRow[]> {
    return this.http.get<InventoryRow[]>(`${this.baseUrl}/inventory`);
  }

  listLowStock(): Observable<InventoryRow[]> {
    return this.http.get<InventoryRow[]>(`${this.baseUrl}/inventory/low-stock`);
  }

  restock(variantId: number, quantity: number, reason: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/inventory/${variantId}/restock`, { quantity, reason });
  }

  listCoupons(): Observable<Coupon[]> {
    return this.http.get<Coupon[]>(`${this.baseUrl}/coupons`);
  }

  createCoupon(coupon: Partial<Coupon>): Observable<Coupon> {
    return this.http.post<Coupon>(`${this.baseUrl}/coupons`, coupon);
  }

  deactivateCoupon(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/coupons/${id}`);
  }
}
