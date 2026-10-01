import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { Order } from '../../../core/models/order.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

const STATUS_OPTIONS = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED'];

@Component({
  selector: 'app-admin-orders',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-orders.component.html',
  styleUrl: './admin-orders.component.scss',
})
export class AdminOrdersComponent {
  private readonly adminService = inject(AdminService);

  readonly orders = signal<Order[]>([]);
  readonly isLoading = signal(true);
  readonly totalPages = signal(0);
  readonly page = signal(0);
  readonly orderNumberFilter = signal('');
  readonly emailFilter = signal('');
  readonly statusFilter = signal('');
  readonly statusOptions = STATUS_OPTIONS;

  constructor() {
    this.fetch();
  }

  fetch(): void {
    this.isLoading.set(true);
    this.adminService.listOrders({
      orderNumber: this.orderNumberFilter() || undefined,
      customerEmail: this.emailFilter() || undefined,
      status: this.statusFilter() || undefined,
      page: this.page(),
      pageSize: 20,
    }).subscribe((res) => {
      this.orders.set(res.items);
      this.totalPages.set(res.totalPages);
      this.isLoading.set(false);
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.fetch();
  }

  onPageChange(p: number): void {
    this.page.set(p);
    this.fetch();
  }

  updateStatus(order: Order, newStatus: string): void {
    if (!newStatus || newStatus === order.status) return;
    this.adminService.updateOrderStatus(order.id, newStatus).subscribe(() => this.fetch());
  }
}
