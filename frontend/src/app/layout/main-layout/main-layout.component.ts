import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';
import { ToastService } from '../../shared/services/toast.service';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, FooterComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-navbar></app-navbar>
    <main id="main-content"><router-outlet></router-outlet></main>
    <app-footer></app-footer>

    <div class="toast-host">
      @for (toast of toastService.toasts(); track toast.id) {
        <div class="toast" [class]="toast.type" role="status">
          <span>{{ toast.message }}</span>
          <button type="button" (click)="toastService.dismiss(toast.id)" aria-label="Dismiss">✕</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-host {
      position: fixed; bottom: 24px; left: 50%; transform: translateX(-50%); z-index: 200;
      display: flex; flex-direction: column; gap: 8px; align-items: center;
    }
    .toast {
      display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-radius: var(--radius-md);
      color: #fff; box-shadow: var(--shadow-md); font-size: 0.875rem; max-width: 90vw;
    }
    .toast.info { background: var(--color-ink); }
    .toast.error { background: var(--color-danger); }
    .toast.success { background: var(--color-success); }
    .toast button { background: none; border: none; color: rgba(255,255,255,0.8); cursor: pointer; }
  `],
})
export class MainLayoutComponent {
  readonly toastService = inject(ToastService);
}
