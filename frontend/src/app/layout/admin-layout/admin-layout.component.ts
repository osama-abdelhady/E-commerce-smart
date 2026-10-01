import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="admin-shell">
      <aside class="sidebar">
        <div class="brand">AureliaSuits <span>Admin</span></div>
        <nav>
          <a routerLink="/admin/dashboard" routerLinkActive="active">Dashboard</a>
          <a routerLink="/admin/products" routerLinkActive="active">Products</a>
          <a routerLink="/admin/categories" routerLinkActive="active">Categories</a>
          <a routerLink="/admin/orders" routerLinkActive="active">Orders</a>
          <a routerLink="/admin/customers" routerLinkActive="active">Customers</a>
          <a routerLink="/admin/inventory" routerLinkActive="active">Inventory</a>
          <a routerLink="/admin/coupons" routerLinkActive="active">Coupons</a>
        </nav>
        <a routerLink="/" class="back-link">← Back to store</a>
      </aside>
      <main class="admin-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [`
    .admin-shell { display: grid; grid-template-columns: 240px 1fr; min-height: 100vh; }
    .sidebar { background: var(--color-ink); color: #d1d5db; padding: 24px 16px; display: flex; flex-direction: column; }
    .brand { color: #fff; font-weight: 800; margin-bottom: 32px; }
    .brand span { color: var(--color-accent); font-weight: 600; font-size: 0.8rem; }
    nav { display: flex; flex-direction: column; gap: 4px; flex: 1; }
    nav a { color: #9ca3af; padding: 10px 12px; border-radius: var(--radius-sm); font-size: 0.9rem; }
    nav a:hover { background: rgba(255,255,255,0.06); color: #fff; text-decoration: none; }
    nav a.active { background: var(--color-primary); color: #fff; }
    .back-link { color: #9ca3af; font-size: 0.8rem; margin-top: 24px; }
    .admin-content { padding: 32px; background: var(--color-surface-alt); }
    @media (max-width: 900px) { .admin-shell { grid-template-columns: 1fr; } .sidebar { display: none; } }
  `],
})
export class AdminLayoutComponent {}
