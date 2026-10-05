import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Único destino após um login bem-sucedido (UC-M10-15, regra 5): nunca a página originalmente pedida. */
export const POST_LOGIN_PATH = '/dashboard';

export const SESSION_TOKEN_KEY = 'gravita.sessionToken';

/** `unavailable` = falha de rede/servidor; nunca vira texto de "senha incorreta". */
export type LoginOutcome = 'authenticated' | 'totp-required' | 'rejected' | 'unavailable';

interface AuthResponse {
  status?: 'AUTHENTICATED' | 'TOTP_REQUIRED' | 'REJECTED';
  sessionToken?: string | null;
}

/** Quem está logado, como devolvido por GET /api/auth/me. `profile` é o nome do perfil de acesso. */
export interface CurrentUser {
  name: string;
  email: string;
  profile: string | null;
}

/** Fonte única de "existe sessão?" para o guard e para a tela de login. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly token = signal<string | null>(this.readStoredToken());
  readonly isAuthenticated = computed(() => !!this.token());

  private readonly user = signal<CurrentUser | null>(null);
  /** O usuário da sessão atual; `null` até {@link loadCurrentUser} concluir ou sem sessão. */
  readonly currentUser = this.user.asReadonly();

  /**
   * GET /api/auth/me — carrega quem está logado. Sessão que o servidor não reconhece (401) é encerrada;
   * qualquer outra falha mantém a sessão e apenas deixa o usuário sem identificação na tela.
   */
  async loadCurrentUser(): Promise<void> {
    const token = this.token();
    if (!token) {
      this.user.set(null);
      return;
    }
    try {
      const user = await firstValueFrom(
        this.http.get<CurrentUser>(`${this.baseUrl}/me`, { headers: { Authorization: `Bearer ${token}` } }));
      this.user.set(user);
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 401) this.clearSession();
    }
  }

  /** POST /api/auth/login */
  login(email: string, password: string): Promise<LoginOutcome> {
    return this.authenticate('login', { email, password });
  }

  /** POST /api/auth/2fa/verify — repete e-mail e senha da primeira etapa junto com o código. */
  verifyTotp(email: string, password: string, totpCode: string): Promise<LoginOutcome> {
    return this.authenticate('2fa/verify', { email, password, totpCode });
  }

  private async authenticate(path: string, body: object): Promise<LoginOutcome> {
    try {
      return this.outcomeFor(await firstValueFrom(this.http.post<AuthResponse>(`${this.baseUrl}/${path}`, body)));
    } catch (error) {
      // 401 {status: REJECTED} é o contrato do backend; qualquer outra falha é indisponibilidade.
      return error instanceof HttpErrorResponse && (error.status === 401 || error.error?.status === 'REJECTED')
        ? 'rejected'
        : 'unavailable';
    }
  }

  private outcomeFor(response: AuthResponse | null): LoginOutcome {
    if (response?.status === 'TOTP_REQUIRED') return 'totp-required';
    if (response?.status === 'AUTHENTICATED' && response.sessionToken) {
      this.storeToken(response.sessionToken);
      return 'authenticated';
    }
    return response?.status === 'REJECTED' ? 'rejected' : 'unavailable';
  }

  private storeToken(token: string) {
    this.token.set(token);
    try { sessionStorage.setItem(SESSION_TOKEN_KEY, token); } catch { /* armazenamento bloqueado: a sessão vale só em memória */ }
  }

  private clearSession() {
    this.token.set(null);
    this.user.set(null);
    try { sessionStorage.removeItem(SESSION_TOKEN_KEY); } catch { /* armazenamento bloqueado: nada a limpar */ }
  }

  private readStoredToken(): string | null {
    try { return sessionStorage.getItem(SESSION_TOKEN_KEY); } catch { return null; }
  }
}
