import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { Coupon } from '../../../core/models/admin.model';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-admin-coupons',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-coupons.component.html',
  styleUrl: './admin-coupons.component.scss',
})
export class AdminCouponsComponent {
  private readonly adminService = inject(AdminService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly coupons = signal<Coupon[]>([]);
  readonly isFormOpen = signal(false);

  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    discountType: ['PERCENTAGE', Validators.required],
    discountValue: [10, [Validators.required, Validators.min(0.01)]],
    minimumOrderAmount: [0],
    maxRedemptions: [null as number | null],
    maxRedemptionsPerUser: [1, Validators.required],
    validFrom: ['', Validators.required],
    validUntil: ['', Validators.required],
    isActive: [true],
  });

  constructor() { this.fetch(); }

  fetch(): void {
    this.adminService.listCoupons().subscribe((c) => this.coupons.set(c));
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    this.adminService.createCoupon({
      ...raw,
      validFrom: new Date(raw.validFrom).toISOString(),
      validUntil: new Date(raw.validUntil).toISOString(),
    }).subscribe({
      next: () => {
        this.toast.show('Coupon created.', 'success');
        this.isFormOpen.set(false);
        this.form.reset({ discountType: 'PERCENTAGE', discountValue: 10, minimumOrderAmount: 0, maxRedemptionsPerUser: 1, isActive: true });
        this.fetch();
      },
    });
  }

  deactivate(c: Coupon): void {
    this.adminService.deactivateCoupon(c.id).subscribe(() => this.fetch());
  }
}
