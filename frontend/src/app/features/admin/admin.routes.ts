import { Routes } from '@angular/router';
import { AdminLayoutComponent } from '../../layout/admin-layout/admin-layout.component';
import { adminGuard } from '../../core/guards/admin.guard';

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/admin-dashboard.component').then((m) => m.AdminDashboardComponent),
        title: 'Admin — Dashboard',
      },
      {
        path: 'products',
        loadComponent: () => import('./products/admin-products.component').then((m) => m.AdminProductsComponent),
        title: 'Admin — Products',
      },
      {
        path: 'categories',
        loadComponent: () => import('./categories/admin-categories.component').then((m) => m.AdminCategoriesComponent),
        title: 'Admin — Categories',
      },
      {
        path: 'orders',
        loadComponent: () => import('./orders/admin-orders.component').then((m) => m.AdminOrdersComponent),
        title: 'Admin — Orders',
      },
      {
        path: 'customers',
        loadComponent: () => import('./customers/admin-customers.component').then((m) => m.AdminCustomersComponent),
        title: 'Admin — Customers',
      },
      {
        path: 'inventory',
        loadComponent: () => import('./inventory/admin-inventory.component').then((m) => m.AdminInventoryComponent),
        title: 'Admin — Inventory',
      },
      {
        path: 'coupons',
        loadComponent: () => import('./coupons/admin-coupons.component').then((m) => m.AdminCouponsComponent),
        title: 'Admin — Coupons',
      },
    ],
  },
];
