import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminProductService } from '../../../core/services/admin-product.service';
import { CategoryService } from '../../../core/services/category.service';
import { ProductDetail } from '../../../core/models/product.model';
import { Category, Brand } from '../../../core/models/category.model';
import { ToastService } from '../../../shared/services/toast.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-admin-products',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, PaginationComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-products.component.html',
  styleUrl: './admin-products.component.scss',
})
export class AdminProductsComponent {
  private readonly productService = inject(AdminProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);

  readonly products = signal<ProductDetail[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly brands = signal<Brand[]>([]);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly isFormOpen = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly form = this.fb.nonNullable.group({
    sku: ['', Validators.required],
    slug: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    price: [0, [Validators.required, Validators.min(0)]],
    discountPrice: [null as number | null],
    categoryId: [null as number | null, Validators.required],
    brandId: [null as number | null],
    status: ['DRAFT'],
    isBestSeller: [false],
    isNewArrival: [false],
    imageUrl: [''],
    variants: this.fb.array<ReturnType<AdminProductsComponent['buildVariantGroup']>>([]),
  });

  constructor() {
    this.fetch();
    this.categoryService.list().subscribe((c) => this.categories.set(c));
    this.categoryService.getBrands().subscribe((b) => this.brands.set(b));
    this.addVariant();
  }

  get variants(): FormArray {
    return this.form.controls.variants;
  }

  private buildVariantGroup() {
    return this.fb.nonNullable.group({
      sku: ['', Validators.required],
      size: [''],
      color: [''],
      colorHex: ['#000000'],
      priceOverride: [null as number | null],
      initialQuantity: [0, Validators.required],
    });
  }

  addVariant(): void {
    this.variants.push(this.buildVariantGroup());
  }

  removeVariant(index: number): void {
    this.variants.removeAt(index);
  }

  fetch(): void {
    this.productService.list(this.page()).subscribe((res) => {
      this.products.set(res.items);
      this.totalPages.set(res.totalPages);
    });
  }

  onPageChange(p: number): void { this.page.set(p); this.fetch(); }

  openCreateForm(): void {
    this.editingId.set(null);
    this.form.reset({ status: 'DRAFT', isBestSeller: false, isNewArrival: false });
    this.variants.clear();
    this.addVariant();
    this.isFormOpen.set(true);
  }

  openEditForm(product: ProductDetail): void {
    this.editingId.set(product.id);
    this.form.patchValue({
      name: product.name,
      description: product.description,
      price: product.price,
      discountPrice: product.discountPrice,
      status: product.status,
      isBestSeller: product.isBestSeller,
      isNewArrival: product.isNewArrival,
    });
    this.isFormOpen.set(true);
  }

  submit(): void {
    if (this.editingId() !== null) {
      this.submitUpdate();
    } else {
      this.submitCreate();
    }
  }

  submitCreate(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const raw = this.form.getRawValue();
    this.productService.create({
      sku: raw.sku,
      slug: raw.slug,
      name: raw.name,
      description: raw.description,
      price: raw.price,
      discountPrice: raw.discountPrice,
      categoryId: raw.categoryId!,
      brandId: raw.brandId,
      isBestSeller: raw.isBestSeller,
      isNewArrival: raw.isNewArrival,
      images: raw.imageUrl ? [{ url: raw.imageUrl, altText: raw.name, displayOrder: 0 }] : [],
      variants: raw.variants.map((v) => ({
        sku: v.sku, size: v.size, color: v.color, colorHex: v.colorHex,
        priceOverride: v.priceOverride, initialQuantity: v.initialQuantity,
      })),
    }).subscribe({
      next: () => { this.toast.show('Product created.', 'success'); this.isFormOpen.set(false); this.fetch(); },
    });
  }

  submitUpdate(): void {
    const id = this.editingId();
    if (id === null || this.form.invalid) return;
    const raw = this.form.getRawValue();
    this.productService.update(id, {
      name: raw.name, description: raw.description, price: raw.price, discountPrice: raw.discountPrice,
      categoryId: raw.categoryId!, brandId: raw.brandId, status: raw.status,
      isBestSeller: raw.isBestSeller, isNewArrival: raw.isNewArrival,
    }).subscribe({
      next: () => { this.toast.show('Product updated.', 'success'); this.isFormOpen.set(false); this.fetch(); },
    });
  }

  deactivate(product: ProductDetail): void {
    this.productService.deactivate(product.id).subscribe(() => {
      this.toast.show('Product deactivated.', 'success');
      this.fetch();
    });
  }
}
