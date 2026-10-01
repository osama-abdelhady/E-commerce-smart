import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/services/toast.service';

/** Matches backend's ApiErrorResponse (common/ApiErrorResponse.java) exactly. */
interface ApiErrorResponse {
  message: string;
  error: string;
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 0) {
        toast.show('Unable to reach the server. Check your connection.', 'error');
      } else if (error.status >= 500) {
        toast.show('Something went wrong on our end. Please try again shortly.', 'error');
      } else if (error.status !== 401) {
        // 401s are handled by authInterceptor's refresh-and-retry flow —
        // surfacing a toast here too would be noise on a routine refresh.
        const body = error.error as ApiErrorResponse | undefined;
        toast.show(body?.message ?? 'Something went wrong.', 'error');
      }
      return throwError(() => error);
    })
  );
};
