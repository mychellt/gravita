import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { PASSWORD_RESET_NEUTRAL_MESSAGE, PasswordResetService } from '../../../core/services/password-reset.service';
import { AUTH_PATHS } from '../auth-paths';

/** O servidor ignora um segundo pedido para o mesmo e-mail dentro deste prazo; o botão espera junto. */
export const RESEND_COOLDOWN_MS = 60_000;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: '../auth-page.scss'
})
export class ForgotPasswordComponent {
  private readonly service = inject(PasswordResetService);

  readonly loginPath = AUTH_PATHS.login;
  readonly neutralMessage = PASSWORD_RESET_NEUTRAL_MESSAGE;

  readonly email = signal('');
  readonly emailError = signal('');
  /** Falha de rede/servidor: nada foi enviado, então a mensagem neutra NÃO pode aparecer. */
  readonly sendError = signal('');
  readonly sending = signal(false);
  readonly sent = signal(false);
  /** Botão travado durante o prazo de espera ("Enviado!"). */
  readonly coolingDown = signal(false);

  onEmailInput(event: Event) {
    this.email.set((event.target as HTMLInputElement).value);
  }

  async submit(event?: Event) {
    event?.preventDefault();
    if (this.sending() || this.coolingDown()) return;

    const email = this.email().trim();
    this.sendError.set('');
    if (!EMAIL_PATTERN.test(email)) {
      this.emailError.set('Informe um e-mail válido.');
      return;
    }
    this.emailError.set('');

    this.sending.set(true);
    const outcome = await this.service.request(email);
    this.sending.set(false);

    if (outcome === 'invalid-email') {
      this.emailError.set('Informe um e-mail válido.');
    } else if (outcome === 'unavailable') {
      this.sendError.set('Não foi possível enviar agora. Tente novamente em instantes.');
    } else {
      // Sempre o mesmo texto, qualquer que seja o detalhe da resposta.
      this.sent.set(true);
      this.coolingDown.set(true);
      this.track('password_reset_requested');
      setTimeout(() => this.coolingDown.set(false), RESEND_COOLDOWN_MS);
    }
  }

  /** Gancho de analytics: o payload nunca leva o e-mail. */
  private track(name: string) {
    window.dispatchEvent(new CustomEvent('gravita:track', { detail: { name } }));
  }
}
