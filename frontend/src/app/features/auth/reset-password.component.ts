import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: '../auth.component.scss',
  template: `
    <section class="auth-page page-container">
      <div class="auth-card card">
        <h1>Set a new password</h1>

        @if (errorMessage()) { <div class="form-error">{{ errorMessage() }}</div> }

        <form [formGroup]="form" (ngSubmit)="submit()">
          <label for="newPassword">New password</label>
          <input id="newPassword" type="password" formControlName="newPassword" autocomplete="new-password" />
          @if (form.controls.newPassword.touched && form.controls.newPassword.invalid) {
            <div class="field-error">At least 8 characters, with uppercase, lowercase, and a number.</div>
          }
          <button type="submit" class="btn-primary" [disabled]="isSubmitting()">
            {{ isSubmitting() ? 'Saving…' : 'Save New Password' }}
          </button>
        </form>

        <p class="switch-link"><a routerLink="/auth/login">Back to log in</a></p>
      </div>
    </section>
  `,
})
export class ResetPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly isSubmitting = signal(false);
  readonly errorMessage = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(8), Validators.pattern(PASSWORD_PATTERN)]],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) { this.errorMessage.set('This reset link is missing its token.'); return; }

    this.isSubmitting.set(true);
    this.auth.resetPassword(token, this.form.getRawValue().newPassword).subscribe({
      next: () => this.router.navigateByUrl('/auth/login'),
      error: (err) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(err?.error?.message ?? 'This reset link is invalid or has expired.');
      },
    });
  }
}
