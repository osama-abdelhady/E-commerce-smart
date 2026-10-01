import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoryService } from '../../../core/services/category.service';
import { AdminCategoryService } from '../../../core/services/admin-category.service';
import { Category, Brand } from '../../../core/models/category.model';
import { ToastService } from '../../../shared/services/toast.service';

@Component({
  selector: 'app-admin-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-categories.component.html',
  styleUrl: './admin-categories.component.scss',
})
export class AdminCategoriesComponent {
  private readonly categoryService = inject(CategoryService);
  private readonly adminCategoryService = inject(AdminCategoryService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly categories = signal<Category[]>([]);
  readonly brands = signal<Brand[]>([]);
  readonly isCategoryFormOpen = signal(false);
  readonly isBrandFormOpen = signal(false);

  readonly categoryForm = this.fb.nonNullable.group({
    slug: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    imageUrl: [''],
    parentId: [null as number | null],
    isActive: [true],
    displayOrder: [0],
  });

  readonly brandForm = this.fb.nonNullable.group({
    slug: ['', Validators.required],
    name: ['', Validators.required],
    logoUrl: [''],
    isActive: [true],
  });

  constructor() {
    this.fetchCategories();
    this.fetchBrands();
  }

  fetchCategories(): void {
    this.categoryService.list().subscribe((c) => this.categories.set(c));
  }

  fetchBrands(): void {
    this.categoryService.getBrands().subscribe((b) => this.brands.set(b));
  }

  submitCategory(): void {
    if (this.categoryForm.invalid) { this.categoryForm.markAllAsTouched(); return; }
    this.adminCategoryService.createCategory(this.categoryForm.getRawValue()).subscribe({
      next: () => {
        this.toast.show('Category created.', 'success');
        this.isCategoryFormOpen.set(false);
        this.categoryForm.reset({ isActive: true, displayOrder: 0 });
        this.fetchCategories();
      },
    });
  }

  deactivateCategory(c: Category): void {
    this.adminCategoryService.deactivateCategory(c.id).subscribe(() => {
      this.toast.show('Category deactivated.', 'success');
      this.fetchCategories();
    });
  }

  submitBrand(): void {
    if (this.brandForm.invalid) { this.brandForm.markAllAsTouched(); return; }
    this.adminCategoryService.createBrand(this.brandForm.getRawValue()).subscribe({
      next: () => {
        this.toast.show('Brand created.', 'success');
        this.isBrandFormOpen.set(false);
        this.brandForm.reset({ isActive: true });
        this.fetchBrands();
      },
    });
  }

  deactivateBrand(b: Brand): void {
    this.adminCategoryService.deactivateBrand(b.id).subscribe(() => {
      this.toast.show('Brand deactivated.', 'success');
      this.fetchBrands();
    });
  }
}
