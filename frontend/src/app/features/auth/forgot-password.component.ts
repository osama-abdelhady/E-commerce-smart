import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: '../auth.component.scss',
  template: `
    <section class="auth-page page-container">
      <div class="auth-card card">
        <h1>Reset your password</h1>
        <p class="muted">Enter your email and we'll send you a reset link.</p>

        @if (submitted()) {
          <div class="form-error" style="background:#ecfdf5;color:var(--color-success);border-color:#a7f3d0;">
            If an account exists for that email, a reset link is on its way.
          </div>
        } @else {
          <form [formGroup]="form" (ngSubmit)="submit()">
            <label for="email">Email</label>
            <input id="email" type="email" formControlName="email" autocomplete="email" />
            <button type="submit" class="btn-primary" [disabled]="isSubmitting()">
              {{ isSubmitting() ? 'Sending…' : 'Send Reset Link' }}
            </button>
          </form>
        }

        <p class="switch-link"><a routerLink="/auth/login">Back to log in</a></p>
      </div>
    </section>
  `,
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly isSubmitting = signal(false);
  readonly submitted = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  submit(): void {
    if (this.form.invalid) return;
    this.isSubmitting.set(true);
    this.auth.forgotPassword(this.form.getRawValue().email).subscribe({
      next: () => { this.isSubmitting.set(false); this.submitted.set(true); },
      error: () => { this.isSubmitting.set(false); this.submitted.set(true); }, // same UX either way — avoid leaking account existence
    });
  }
}
