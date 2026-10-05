import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService, LoginOutcome, POST_LOGIN_PATH } from '../../../core/services/auth.service';
import { AUTH_PATHS } from '../auth-paths';

/** Mesma frase para senha errada, e-mail desconhecido, conta inativa ou pendente de ativação (regra 2). */
export const LOGIN_REJECTED_MESSAGE = 'E-mail ou senha incorretos. Confira seu e-mail e senha e tente novamente.';
export const TOTP_REJECTED_MESSAGE = 'Código de verificação inválido. Confira o código no seu aplicativo e tente novamente.';
export const LOGIN_UNAVAILABLE_MESSAGE = 'Não foi possível entrar agora. Tente novamente em instantes.';

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const TOTP_PATTERN = /^\d{6}$/;

type Step = 'credentials' | 'totp';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './login.component.html',
  styleUrl: '../auth-page.scss'
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  @ViewChild('emailInput') private emailInput?: ElementRef<HTMLInputElement>;
  @ViewChild('passwordInput') private passwordInput?: ElementRef<HTMLInputElement>;
  @ViewChild('totpInput') private totpInput?: ElementRef<HTMLInputElement>;

  readonly forgotPasswordPath = AUTH_PATHS.forgotPassword;

  readonly step = signal<Step>('credentials');
  readonly email = signal('');
  readonly code = signal('');
  readonly emailError = signal('');
  readonly passwordError = signal('');
  readonly codeError = signal('');
  /** Erro da tentativa em si (rejeição genérica ou indisponibilidade); nunca traz status HTTP. */
  readonly formError = signal('');
  readonly submitting = signal(false);

  /** A senha fica só em memória e nunca é reexibida: a etapa do TOTP a reenvia sem pedir de novo. */
  private password = '';

  onEmailInput(event: Event) { this.email.set((event.target as HTMLInputElement).value); }
  onPasswordInput(event: Event) { this.password = (event.target as HTMLInputElement).value; }
  onCodeInput(event: Event) { this.code.set((event.target as HTMLInputElement).value); }

  async submit(event?: Event) {
    event?.preventDefault();
    if (this.submitting() || !this.validateCredentials()) return;

    this.formError.set('');
    this.submitting.set(true);
    const outcome = await this.auth.login(this.email().trim(), this.password);
    this.submitting.set(false);

    if (outcome === 'authenticated') return this.finish();
    if (outcome === 'totp-required') return this.showTotpStep();
    this.formError.set(outcome === 'rejected' ? LOGIN_REJECTED_MESSAGE : LOGIN_UNAVAILABLE_MESSAGE);
    this.clearPassword();
    this.emailInput?.nativeElement.focus();
  }

  async submitCode(event?: Event) {
    event?.preventDefault();
    if (this.submitting()) return;

    const code = this.code().trim();
    if (!TOTP_PATTERN.test(code)) {
      this.codeError.set('Informe o código de 6 dígitos.');
      return;
    }
    this.codeError.set('');
    this.formError.set('');

    this.submitting.set(true);
    const outcome = await this.auth.verifyTotp(this.email().trim(), this.password, code);
    this.submitting.set(false);

    if (outcome === 'authenticated') return this.finish();
    if (outcome === 'unavailable') {
      this.formError.set(LOGIN_UNAVAILABLE_MESSAGE);
    } else {
      // Nesta etapa a senha já foi aceita, então o erro pode nomear o código (regra 4).
      this.codeError.set(TOTP_REJECTED_MESSAGE);
    }
    this.code.set('');
    this.totpInput?.nativeElement.focus();
  }

  backToCredentials() {
    this.clearPassword();
    this.code.set('');
    this.codeError.set('');
    this.formError.set('');
    this.step.set('credentials');
  }

  private validateCredentials(): boolean {
    const emailOk = EMAIL_PATTERN.test(this.email().trim());
    this.emailError.set(emailOk ? '' : 'Informe um e-mail válido.');
    this.passwordError.set(this.password ? '' : 'Informe sua senha.');
    return emailOk && !!this.password;
  }

  private showTotpStep() {
    this.step.set('totp');
    // O campo só existe depois da renderização do novo passo.
    setTimeout(() => this.totpInput?.nativeElement.focus());
  }

  private finish() {
    this.clearPassword();
    this.router.navigateByUrl(POST_LOGIN_PATH, { replaceUrl: true });
  }

  /** Limpa o valor no campo e na memória, para a senha nunca ser reexibida (inclusive nos erros). */
  private clearPassword() {
    this.password = '';
    if (this.passwordInput) this.passwordInput.nativeElement.value = '';
  }
}
