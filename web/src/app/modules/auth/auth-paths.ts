/** Rotas das telas públicas de autenticação. O link "Esqueci minha senha" do login aponta para `forgotPassword`. */
export const AUTH_PATHS = {
  login: '/login',
  forgotPassword: '/forgot-password',
  resetPassword: '/reset-password',
} as const;
