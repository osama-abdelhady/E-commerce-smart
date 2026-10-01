import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <footer class="site-footer">
      <div class="page-container footer-grid">
        <div>
          <h4>AureliaSuits</h4>
          <p>Contact: support&#64;aureliasuits.example &middot; +1 (555) 010-0100</p>
        </div>
        <div>
          <h5>Shop</h5>
          <a routerLink="/products">All Products</a>
          <a routerLink="/products" [queryParams]="{sortBy:'NEWEST'}">New Arrivals</a>
          <a routerLink="/products" [queryParams]="{sortBy:'POPULARITY'}">Best Sellers</a>
        </div>
        <div>
          <h5>Account</h5>
          <a routerLink="/orders">Track Order</a>
          <a routerLink="/account">My Account</a>
          <a routerLink="/wishlist">Wishlist</a>
        </div>
        <div>
          <h5>Support</h5>
          <a href="#">Shipping &amp; Returns</a>
          <a href="#">Size Guide</a>
          <a href="#">Contact Us</a>
        </div>
      </div>
      <div class="page-container footer-bottom">
        <span>&copy; {{ year }} AureliaSuits. All rights reserved.</span>
      </div>
    </footer>
  `,
  styles: [`
    .site-footer { background: var(--color-ink); color: #d1d5db; margin-top: 64px; }
    .footer-grid { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr; gap: 32px; padding: 48px 0 24px; }
    .footer-grid h4 { color: #fff; }
    .footer-grid h5 { color: #fff; font-size: 0.8rem; text-transform: uppercase; margin-bottom: 12px; }
    .footer-grid a { display: block; color: #9ca3af; margin-bottom: 8px; font-size: 0.875rem; }
    .footer-grid a:hover { color: #fff; text-decoration: none; }
    .footer-bottom { padding: 16px 0 32px; font-size: 0.8rem; color: #9ca3af; border-top: 1px solid #2b2f38; }
    @media (max-width: 700px) { .footer-grid { grid-template-columns: 1fr 1fr; } }
  `],
})
export class FooterComponent {
  readonly year = new Date().getFullYear();
}
