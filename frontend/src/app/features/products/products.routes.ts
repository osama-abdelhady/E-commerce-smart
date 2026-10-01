import { Routes } from '@angular/router';

export const PRODUCTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./list/product-list.component').then((m) => m.ProductListComponent),
    title: 'Shop — AureliaSuits',
  },
  {
    path: ':slug',
    loadComponent: () => import('./detail/product-detail.component').then((m) => m.ProductDetailComponent),
    title: 'Product — AureliaSuits',
  },
];
