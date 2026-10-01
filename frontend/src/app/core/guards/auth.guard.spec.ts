import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authServiceMock: Partial<AuthService>;
  let routerMock: Partial<Router>;
  let createUrlTreeSpy: jasmine.Spy;

  beforeEach(() => {
    createUrlTreeSpy = jasmine.createSpy('createUrlTree').and.returnValue('URL_TREE' as any);
    authServiceMock = { isAuthenticated: () => false };
    routerMock = { createUrlTree: createUrlTreeSpy };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
      ],
    });
  });

  function runGuard(url: string): unknown {
    return TestBed.runInInjectionContext(() =>
      authGuard(
        {} as any,
        { url } as any
      )
    );
  }

  it('allows navigation when the user is authenticated', () => {
    authServiceMock.isAuthenticated = () => true;
    const result = runGuard('/account');
    expect(result).toBe(true);
  });

  it('redirects to /auth/login when the user is not authenticated', () => {
    authServiceMock.isAuthenticated = () => false;
    runGuard('/checkout');
    expect(createUrlTreeSpy).toHaveBeenCalledWith(['/auth/login'], { queryParams: { redirectTo: '/checkout' } });
  });
});
