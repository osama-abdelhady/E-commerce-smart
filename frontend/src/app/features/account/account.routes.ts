import { Routes } from '@angular/router';
import { ComingSoonComponent } from '../../shared/components/coming-soon/coming-soon.component';
import { authGuard } from '../../core/guards/auth.guard';

export const ACCOUNT_ROUTES: Routes = [
  {
    path: '',
    component: ComingSoonComponent,
    canActivate: [authGuard],
    title: 'My Account',
    data: { title: 'My Account', description: 'Profile settings and address book wire up here next, against AccountService.' },
  },
];
