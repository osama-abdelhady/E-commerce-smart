import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Brand, Category } from '../models/category.model';

export interface CategoryPayload {
  slug: string; name: string; description: string; imageUrl: string;
  parentId: number | null; isActive: boolean; displayOrder: number;
}

export interface BrandPayload {
  slug: string; name: string; logoUrl: string; isActive: boolean;
}

@Injectable({ providedIn: 'root' })
export class AdminCategoryService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/admin`;

  createCategory(payload: CategoryPayload): Observable<Category> {
    return this.http.post<Category>(`${this.baseUrl}/categories`, payload);
  }

  updateCategory(id: number, payload: CategoryPayload): Observable<Category> {
    return this.http.put<Category>(`${this.baseUrl}/categories/${id}`, payload);
  }

  deactivateCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/categories/${id}`);
  }

  createBrand(payload: BrandPayload): Observable<Brand> {
    return this.http.post<Brand>(`${this.baseUrl}/brands`, payload);
  }

  updateBrand(id: number, payload: BrandPayload): Observable<Brand> {
    return this.http.put<Brand>(`${this.baseUrl}/brands/${id}`, payload);
  }

  deactivateBrand(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/brands/${id}`);
  }
}
