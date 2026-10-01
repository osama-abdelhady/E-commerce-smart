import { Routes } from '@angular/router';
import { ComingSoonComponent } from '../../shared/components/coming-soon/coming-soon.component';
import { authGuard } from '../../core/guards/auth.guard';

export const ORDERS_ROUTES: Routes = [
  {
    path: '',
    component: ComingSoonComponent,
    canActivate: [authGuard],
    title: 'Order History',
    data: { title: 'Order History', description: 'Order list + detail with status timeline wires up here next, against OrderService.' },
  },
];
