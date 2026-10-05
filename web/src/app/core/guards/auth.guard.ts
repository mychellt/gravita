import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AUTH_PATHS } from '../../modules/auth/auth-paths';
import { AuthService, POST_LOGIN_PATH } from '../services/auth.service';

/** Rotas autenticadas: sem sessão, a visita vai para o login e a tela protegida nunca é renderizada. */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService).isAuthenticated() ? true : router.createUrlTree([AUTH_PATHS.login]);
};

/** Tela de login: com sessão ativa, segue direto para o dashboard. */
export const guestGuard: CanActivateFn = () => {
  const router = inject(Router);
  return inject(AuthService).isAuthenticated() ? router.createUrlTree([POST_LOGIN_PATH]) : true;
};
