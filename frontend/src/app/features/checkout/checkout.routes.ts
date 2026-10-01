import { Routes } from '@angular/router';
import { ComingSoonComponent } from '../../shared/components/coming-soon/coming-soon.component';
import { authGuard } from '../../core/guards/auth.guard';

export const CHECKOUT_ROUTES: Routes = [
  {
    path: '',
    component: ComingSoonComponent,
    canActivate: [authGuard],
    title: 'Checkout',
    data: {
      title: 'Checkout',
      description: 'The multi-step checkout (address, coupon, Stripe payment via @stripe/stripe-js, order review) wires up here next, against the checkout()/payments endpoints already built in the backend.',
    },
  },
  { path: 'success', component: ComingSoonComponent, title: 'Order Confirmed' },
];
