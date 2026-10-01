import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Address, AddressRequest, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/users/me`;

  getProfile(): Observable<User> {
    return this.http.get<User>(this.baseUrl);
  }

  updateProfile(fullName: string, phone?: string): Observable<User> {
    return this.http.put<User>(this.baseUrl, { fullName, phone });
  }

  listAddresses(): Observable<Address[]> {
    return this.http.get<Address[]>(`${this.baseUrl}/addresses`);
  }

  addAddress(request: AddressRequest): Observable<Address> {
    return this.http.post<Address>(`${this.baseUrl}/addresses`, request);
  }

  updateAddress(id: number, request: AddressRequest): Observable<Address> {
    return this.http.put<Address>(`${this.baseUrl}/addresses/${id}`, request);
  }

  deleteAddress(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/addresses/${id}`);
  }
}
