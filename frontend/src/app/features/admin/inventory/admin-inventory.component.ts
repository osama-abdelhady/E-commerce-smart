import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { InventoryRow } from '../../../core/models/admin.model';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-admin-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1>Inventory</h1>
    <div class="filter-bar">
      <button type="button" class="btn-secondary" [class.active]="!lowStockOnly()" (click)="showAll()">All</button>
      <button type="button" class="btn-secondary" [class.active]="lowStockOnly()" (click)="showLowStock()">Low Stock Only</button>
    </div>

    <table class="data-table card">
      <thead><tr><th>Product</th><th>Variant SKU</th><th>Available</th><th>Reserved</th><th>Sellable</th><th>Threshold</th><th>Restock</th></tr></thead>
      <tbody>
        @for (row of rows(); track row.id) {
          <tr [class.low]="row.isLowStock">
            <td>{{ row.productName }}</td>
            <td>{{ row.variantSku }}</td>
            <td>{{ row.quantityAvailable }}</td>
            <td>{{ row.quantityReserved }}</td>
            <td>{{ row.sellable }}</td>
            <td>{{ row.lowStockThreshold }}</td>
            <td class="restock-cell">
              <input type="number" min="1" [(ngModel)]="restockAmounts[row.id]" placeholder="Qty" />
              <button type="button" class="link-btn" (click)="restock(row)">Add Stock</button>
            </td>
          </tr>
        }
        @if (rows().length === 0) { <tr><td colspan="7">Nothing to show.</td></tr> }
      </tbody>
    </table>
  `,
  styles: [`
    :host { display: block; }
    .filter-bar { display: flex; gap: 8px; margin-bottom: 20px; }
    .filter-bar button.active { background: var(--color-primary); color: #fff; }
    .data-table { padding: 8px; }
    tr.low { background: #fef2f2; }
    .restock-cell { display: flex; gap: 6px; align-items: center; }
    .restock-cell input { width: 60px; padding: 4px 6px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); }
    .link-btn { background: none; border: none; color: var(--color-primary); cursor: pointer; text-decoration: underline; font-size: 0.8125rem; }
  `],
})
export class AdminInventoryComponent {
  private readonly adminService = inject(AdminService);
  private readonly toast = inject(ToastService);

  readonly rows = signal<InventoryRow[]>([]);
  readonly lowStockOnly = signal(false);
  readonly restockAmounts: Record<number, number> = {};

  constructor() { this.showAll(); }

  showAll(): void {
    this.lowStockOnly.set(false);
    this.adminService.listInventory().subscribe((rows) => this.rows.set(rows));
  }

  showLowStock(): void {
    this.lowStockOnly.set(true);
    this.adminService.listLowStock().subscribe((rows) => this.rows.set(rows));
  }

  restock(row: InventoryRow): void {
    const qty = this.restockAmounts[row.id];
    if (!qty || qty < 1) { this.toast.show('Enter a quantity to add.', 'error'); return; }
    this.adminService.restock(row.productVariantId, qty, 'Manual restock via admin dashboard').subscribe(() => {
      this.toast.show('Stock updated.', 'success');
      this.lowStockOnly() ? this.showLowStock() : this.showAll();
    });
  }
}
