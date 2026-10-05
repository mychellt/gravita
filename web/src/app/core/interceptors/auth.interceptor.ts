import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { environment } from '../../../environments/environment';
import { AuthService } from '../services/auth.service';

/** Chamadas de autenticação são anônimas: o login nunca carrega uma sessão antiga. */
const ANONYMOUS_PATHS = [`${environment.apiUrl}/auth/login`, `${environment.apiUrl}/auth/2fa`];

/**
 * Envia `Authorization: Bearer <sessionToken>` nas chamadas à API que exigem um usuário autenticado
 * (ex.: `/api/customers`). Não toca em chamadas de outras origens, nas de login/2FA nem nas que já trazem o cabeçalho.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const isApiCall = request.url.startsWith(`${environment.apiUrl}/`);
  const isAnonymous = ANONYMOUS_PATHS.some(path => request.url.startsWith(path));
  const token = inject(AuthService).sessionToken();

  if (!isApiCall || isAnonymous || !token || request.headers.has('Authorization')) return next(request);
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
