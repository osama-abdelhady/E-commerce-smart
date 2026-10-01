import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { DashboardSummary } from '../../../core/models/admin.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
})
export class AdminDashboardComponent {
  private readonly adminService = inject(AdminService);
  readonly summary = signal<DashboardSummary | null>(null);
  readonly isLoading = signal(true);

  constructor() {
    this.adminService.getDashboard().subscribe({
      next: (s) => { this.summary.set(s); this.isLoading.set(false); },
      error: () => this.isLoading.set(false),
    });
  }
}
