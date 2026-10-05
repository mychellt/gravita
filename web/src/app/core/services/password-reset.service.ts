import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Resposta única do pedido de redefinição: vale para qualquer e-mail, com ou sem conta (privacidade). */
export const PASSWORD_RESET_NEUTRAL_MESSAGE =
  'Se este e-mail tiver uma conta, enviaremos um link de redefinição em instantes.';

export type RequestOutcome = 'accepted' | 'invalid-email' | 'unavailable';

/** `used` = link já utilizado; `rejected` = a senha foi recusada e o link continua valendo. */
export type ConfirmOutcome = 'success' | 'expired' | 'used' | 'invalid' | 'rejected' | 'unavailable';

@Injectable({ providedIn: 'root' })
export class PasswordResetService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth/password-reset`;

  /** POST /api/auth/password-reset — 202 sempre, 400 só para endereço malformado. */
  async request(email: string): Promise<RequestOutcome> {
    try {
      await firstValueFrom(this.http.post(this.baseUrl, { email }));
      return 'accepted';
    } catch (error) {
      return error instanceof HttpErrorResponse && error.status === 400 ? 'invalid-email' : 'unavailable';
    }
  }

  /** POST /api/auth/password-reset/confirm — o token vai no corpo, nunca na URL. */
  async confirm(token: string, newPassword: string): Promise<ConfirmOutcome> {
    try {
      await firstValueFrom(this.http.post(`${this.baseUrl}/confirm`, { token, newPassword }));
      return 'success';
    } catch (error) {
      return error instanceof HttpErrorResponse ? this.outcomeFor(error) : 'unavailable';
    }
  }

  /** 410 = link real, mas vencido ou já usado; 400 com `reason` = link desconhecido; 400 sem `reason` = senha recusada. */
  private outcomeFor(error: HttpErrorResponse): ConfirmOutcome {
    const reason = error.error?.reason;
    if (error.status === 410) {
      return reason === 'EXPIRED' ? 'expired' : reason === 'ALREADY_USED' ? 'used' : 'invalid';
    }
    if (error.status === 400) {
      return reason ? 'invalid' : 'rejected';
    }
    return 'unavailable';
  }
}
