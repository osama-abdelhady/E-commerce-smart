import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { CustomerSummary } from '../../../core/models/admin.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-admin-customers',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1>Customers</h1>
    <div class="filter-bar card">
      <input type="text" placeholder="Search by name or email" [(ngModel)]="search" (keyup.enter)="fetch()" />
      <button type="button" class="btn-primary" (click)="fetch()">Search</button>
    </div>

    <table class="data-table card">
      <thead><tr><th>Name</th><th>Email</th><th>Status</th><th>Joined</th><th>Orders</th><th>Actions</th></tr></thead>
      <tbody>
        @for (c of customers(); track c.id) {
          <tr>
            <td>{{ c.fullName }}</td>
            <td>{{ c.email }}</td>
            <td>{{ c.status }}</td>
            <td>{{ c.createdAt | date:'mediumDate' }}</td>
            <td>{{ c.orderCount }}</td>
            <td>
              @if (c.status === 'ACTIVE') {
                <button type="button" class="link-btn" (click)="deactivate(c)">Deactivate</button>
              } @else {
                <button type="button" class="link-btn" (click)="activate(c)">Activate</button>
              }
            </td>
          </tr>
        }
        @if (customers().length === 0) { <tr><td colspan="6">No customers found.</td></tr> }
      </tbody>
    </table>
    <app-pagination [currentPage]="page()" [totalPages]="totalPages()" (pageChange)="onPageChange($event)"></app-pagination>
  `,
  styles: [`
    :host { display: block; }
    .filter-bar { display: flex; gap: 12px; padding: 16px; margin-bottom: 20px; }
    .filter-bar input { flex: 1; max-width: 320px; padding: 8px 10px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); }
    .data-table { padding: 8px; }
    .link-btn { background: none; border: none; color: var(--color-primary); cursor: pointer; text-decoration: underline; font-size: 0.8125rem; }
  `],
})
export class AdminCustomersComponent {
  private readonly adminService = inject(AdminService);
  readonly customers = signal<CustomerSummary[]>([]);
  readonly search = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);

  constructor() { this.fetch(); }

  fetch(): void {
    this.adminService.listCustomers(this.search() || undefined, this.page()).subscribe((res) => {
      this.customers.set(res.items);
      this.totalPages.set(res.totalPages);
    });
  }

  onPageChange(p: number): void { this.page.set(p); this.fetch(); }

  activate(c: CustomerSummary): void { this.adminService.activateCustomer(c.id).subscribe(() => this.fetch()); }
  deactivate(c: CustomerSummary): void { this.adminService.deactivateCustomer(c.id).subscribe(() => this.fetch()); }
}
