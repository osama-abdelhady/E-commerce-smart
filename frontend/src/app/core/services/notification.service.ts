import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AppNotification, PageResponse } from '../models/misc.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/notifications`;

  readonly unreadCount = signal(0);

  list(page = 0, pageSize = 20): Observable<PageResponse<AppNotification>> {
    return this.http.get<PageResponse<AppNotification>>(this.baseUrl, { params: { page, pageSize } });
  }

  refreshUnreadCount(): Observable<number> {
    return this.http
      .get<number>(`${this.baseUrl}/unread-count`)
      .pipe(tap((count) => this.unreadCount.set(count)));
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http.post<AppNotification>(`${this.baseUrl}/${id}/read`, {});
  }
}
