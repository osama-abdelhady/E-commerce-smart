import { Routes } from '@angular/router';
import { ComingSoonComponent } from '../../shared/components/coming-soon/coming-soon.component';
import { authGuard } from '../../core/guards/auth.guard';

export const WISHLIST_ROUTES: Routes = [
  {
    path: '',
    component: ComingSoonComponent,
    canActivate: [authGuard],
    title: 'Wishlist',
    data: { title: 'Wishlist', description: 'A grid view over WishlistService, with move-to-cart, wires up here next.' },
  },
];
