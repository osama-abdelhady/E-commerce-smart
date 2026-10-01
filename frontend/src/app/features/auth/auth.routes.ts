import { Routes } from '@angular/router';

export const AUTH_ROUTES: Routes = [
  { path: 'login', loadComponent: () => import('./login/login.component').then((m) => m.LoginComponent), title: 'Log In' },
  { path: 'register', loadComponent: () => import('./register/register.component').then((m) => m.RegisterComponent), title: 'Create Account' },
  { path: 'forgot-password', loadComponent: () => import('./forgot-password.component').then((m) => m.ForgotPasswordComponent), title: 'Reset Password' },
  { path: 'reset-password', loadComponent: () => import('./reset-password.component').then((m) => m.ResetPasswordComponent), title: 'Set New Password' },
];
