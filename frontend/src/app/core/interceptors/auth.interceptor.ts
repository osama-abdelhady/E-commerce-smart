import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

const AUTH_EXEMPT_PATHS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/forgot-password', '/auth/reset-password'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getAccessToken();
  const isExempt = AUTH_EXEMPT_PATHS.some((p) => req.url.includes(p));

  const authorizedReq = token && !isExempt
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` }, withCredentials: true })
    : req.clone({ withCredentials: true });

  return next(authorizedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // A 401 on anything other than the auth endpoints themselves means the
      // access token expired — silently refresh via the HttpOnly cookie and
      // retry the original request exactly once.
      if (error.status === 401 && !isExempt) {
        return auth.refresh().pipe(
          switchMap(() => {
            const retried = req.clone({
              setHeaders: { Authorization: `Bearer ${auth.getAccessToken()}` },
              withCredentials: true,
            });
            return next(retried);
          }),
          catchError((refreshError) => throwError(() => refreshError))
        );
      }
      return throwError(() => error);
    })
  );
};
