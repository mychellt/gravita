/** Iniciais para o avatar: primeira letra do primeiro e do último nome ("Ana Beatriz Souza" → "AS"). */
export function initialsOf(name: string | null | undefined): string {
  const parts = (name ?? '').trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  const first = parts[0][0];
  const last = parts.length > 1 ? parts[parts.length - 1][0] : '';
  return (first + last).toLocaleUpperCase('pt-BR');
}

/** Nome do perfil de acesso com permissão total, como cadastrado no backend. */
export const ADMINISTRATOR_PROFILE = 'Administrator';

const PROFILE_LABELS: Record<string, string> = {
  [ADMINISTRATOR_PROFILE]: 'Administrador',
  'Financial': 'Financeiro',
  'Salesperson': 'Vendedor',
  'Cashier Operator': 'Operador de caixa',
  'Purchasing': 'Compras',
  'Read-only': 'Somente leitura',
};

/** Perfis padrão têm nome em inglês no cadastro; a tela os mostra em português. Perfis personalizados passam como estão. */
export function profileLabel(profile: string | null | undefined): string {
  return profile ? PROFILE_LABELS[profile] ?? profile : '';
}
