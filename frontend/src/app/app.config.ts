import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { catchError, of, tap } from 'rxjs';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';
import { AuthService } from './core/services/auth.service';
import { CartService } from './core/services/cart.service';
import { WishlistService } from './core/services/wishlist.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled', anchorScrolling: 'enabled' })
    ),
    provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
    provideAnimations(),
    // Silently attempts to re-establish a session from the HttpOnly refresh
    // cookie on a hard page load (the access token itself is in-memory only
    // and is lost on refresh by design — see AuthService). A failure here
    // just means the visitor is logged out, which is the normal case for
    // most page loads — never blocks app startup.
    provideAppInitializer(() => {
      const auth = inject(AuthService);
      const cart = inject(CartService);
      const wishlist = inject(WishlistService);

      return auth.refresh().pipe(
        tap(() => {
          if (auth.isAuthenticated()) {
            cart.load().subscribe();
            wishlist.load().subscribe();
          }
        }),
        catchError(() => of(null))
      );
    }),
  ],
};
