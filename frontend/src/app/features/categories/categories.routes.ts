import { Routes } from '@angular/router';

export const CATEGORIES_ROUTES: Routes = [
  {
    path: ':slug',
    loadComponent: () => import('./category-redirect.component').then((m) => m.CategoryRedirectComponent),
  },
];
