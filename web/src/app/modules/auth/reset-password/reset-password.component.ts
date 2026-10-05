import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConfirmOutcome, PasswordResetService } from '../../../core/services/password-reset.service';
import { AUTH_PATHS } from '../auth-paths';

/** `form` é a tela de nova senha; os demais são o resultado final da visita. */
export type ResetView = 'form' | 'success' | 'expired' | 'invalid';

interface ResultCopy {
  icon: string;
  tone: 'ok' | 'bad';
  title: string;
  message: string;
}

/** Textos por estado (UC-M10-16). Só o sucesso diz que a senha mudou; "expirado" nunca é chamado de "inválido". */
const RESULTS: Record<Exclude<ResetView, 'form'>, ResultCopy> = {
  success: {
    icon: 'ti-check', tone: 'ok',
    title: 'Senha redefinida',
    message: 'Tudo certo! Sua senha foi alterada. Entre com a nova senha.',
  },
  expired: {
    icon: 'ti-clock-exclamation', tone: 'bad',
    title: 'Link expirado',
    message: 'Este link de redefinição expirou. Peça um novo para continuar.',
  },
  invalid: {
    icon: 'ti-alert-triangle', tone: 'bad',
    title: 'Link inválido',
    message: 'Este link de redefinição não é válido ou já foi utilizado. Peça um novo para continuar.',
  },
};

const ANALYTICS: Record<Exclude<ConfirmOutcome, 'rejected' | 'unavailable'>, string> = {
  success: 'password_reset_succeeded',
  expired: 'password_reset_link_expired',
  used: 'password_reset_link_used',
  invalid: 'password_reset_link_invalid',
};

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: '../auth-page.scss'
})
export class ResetPasswordComponent implements OnInit {
  private readonly service = inject(PasswordResetService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** O token é um segredo de uso único: vive só aqui, em memória, e sai da barra de endereço ao ler. */
  private token: string | null = null;

  /** Leitores de tela anunciam o título do resultado: o foco vai para ele assim que aparece. */
  @ViewChild('title') set title(heading: ElementRef<HTMLElement> | undefined) {
    heading?.nativeElement.focus();
  }

  readonly loginPath = AUTH_PATHS.login;
  readonly forgotPath = AUTH_PATHS.forgotPassword;

  readonly view = signal<ResetView>('form');
  readonly password = signal('');
  readonly confirmation = signal('');
  readonly passwordError = signal('');
  readonly confirmationError = signal('');
  readonly submitError = signal('');
  readonly submitting = signal(false);

  readonly result = (view: ResetView) => (view === 'form' ? null : RESULTS[view]);

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token')?.trim() || null;
    // Fora do histórico e da URL copiável (critério 12).
    void this.router.navigate([], { relativeTo: this.route, queryParams: { token: null }, queryParamsHandling: 'merge', replaceUrl: true });

    if (!this.token) {
      this.show('invalid', 'password_reset_link_invalid');
    }
  }

  onPasswordInput(event: Event) {
    this.password.set((event.target as HTMLInputElement).value);
  }

  onConfirmationInput(event: Event) {
    this.confirmation.set((event.target as HTMLInputElement).value);
  }

  async submit(event?: Event) {
    event?.preventDefault();
    if (this.submitting() || !this.token || !this.validate()) return;

    this.submitting.set(true);
    this.submitError.set('');
    const outcome = await this.service.confirm(this.token, this.password());
    this.submitting.set(false);

    switch (outcome) {
      case 'success': return this.show('success', ANALYTICS.success);
      case 'expired': return this.show('expired', ANALYTICS.expired);
      case 'used': return this.show('invalid', ANALYTICS.used);
      case 'invalid': return this.show('invalid', ANALYTICS.invalid);
      case 'rejected': return this.submitError.set('Não foi possível usar essa senha. Escolha outra e tente de novo.');
      default: return this.submitError.set('Não foi possível redefinir agora. Tente novamente em instantes.');
    }
  }

  /** Checagem no cliente antes de enviar: só exige a senha e que a confirmação confira (sem política de força). */
  private validate(): boolean {
    this.passwordError.set(this.password().trim() ? '' : 'Informe a nova senha.');
    this.confirmationError.set(
      this.password() === this.confirmation() ? '' : 'As senhas não conferem.');
    return !this.passwordError() && !this.confirmationError();
  }

  private show(view: Exclude<ResetView, 'form'>, event: string) {
    // O resultado é final: não há mais o que fazer com o token nem com as senhas digitadas.
    this.token = null;
    this.password.set('');
    this.confirmation.set('');
    this.view.set(view);
    window.dispatchEvent(new CustomEvent('gravita:track', { detail: { name: event } }));
  }
}
