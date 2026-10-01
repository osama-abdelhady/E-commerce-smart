import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-category-redirect',
  standalone: true,
  template: '',
})
export class CategoryRedirectComponent {
  constructor() {
    const route = inject(ActivatedRoute);
    const router = inject(Router);
    const slug = route.snapshot.paramMap.get('slug')!;
    router.navigate(['/products'], { queryParams: { categorySlug: slug }, replaceUrl: true });
  }
}
