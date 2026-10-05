import { initialsOf, profileLabel } from './user-display';

describe('user display helpers', () => {
  it('uses the first and last name initials', () => {
    expect(initialsOf('Ana Beatriz Souza')).toBe('AS');
    expect(initialsOf('  mariana   lima ')).toBe('ML');
  });

  it('falls back to a single initial or a placeholder', () => {
    expect(initialsOf('Ana')).toBe('A');
    expect(initialsOf('')).toBe('?');
    expect(initialsOf(null)).toBe('?');
  });

  it('shows the standard profiles in Portuguese and custom ones as they are', () => {
    expect(profileLabel('Administrator')).toBe('Administrador');
    expect(profileLabel('Cashier Operator')).toBe('Operador de caixa');
    expect(profileLabel('Gerente de Loja')).toBe('Gerente de Loja');
    expect(profileLabel(null)).toBe('');
  });
});
