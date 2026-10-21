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

/** Perfil dos funcionários da Gravita, o único com acesso à área administrativa (`/admin`). */
/** Destino do GRAVITA_ADMINISTRATOR depois do login e ao tentar a área das empresas. */
export const ADMIN_HOME_PATH = '/admin';

export const GRAVITA_ADMINISTRATOR_PROFILE = 'GRAVITA_ADMINISTRATOR';

/**
 * Área administrativa da plataforma: só o perfil GRAVITA_ADMINISTRATOR entra; os demais voltam ao dashboard.
 * Sem sessão vai para o login. O servidor repete a checagem em cada chamada `/api/admin/**`.
 */
export const adminGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const auth = inject(AuthService);
  if (!auth.isAuthenticated()) return router.createUrlTree([AUTH_PATHS.login]);
  if (!auth.currentUser()) await auth.loadCurrentUser();
  if (!auth.isAuthenticated()) return router.createUrlTree([AUTH_PATHS.login]);
  return auth.currentUser()?.profile === GRAVITA_ADMINISTRATOR_PROFILE ? true : router.createUrlTree([POST_LOGIN_PATH]);
};

/**
 * Área das empresas (ERP): o GRAVITA_ADMINISTRATOR não entra, só na área administrativa — vai direto para `/admin`.
 * Sem sessão, o authGuard (antes deste na rota) já mandou para o login.
 */
export const tenantAreaGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const auth = inject(AuthService);
  if (!auth.isAuthenticated()) return true;
  if (!auth.currentUser()) await auth.loadCurrentUser();
  return auth.currentUser()?.profile === GRAVITA_ADMINISTRATOR_PROFILE ? router.createUrlTree([ADMIN_HOME_PATH]) : true;
};
