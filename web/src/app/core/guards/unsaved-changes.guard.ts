import { CanDeactivateFn } from '@angular/router';

export interface HasUnsavedChanges {
  hasUnsavedChanges(): boolean;
  /** Prefixo de URL dentro do qual a navegação mantém o rascunho (ex.: troca de seção). */
  readonly draftScope?: string;
}

export const unsavedChangesGuard: CanDeactivateFn<HasUnsavedChanges> = (component, _route, _state, next) => {
  if (!component.hasUnsavedChanges()) return true;
  if (component.draftScope && next.url.startsWith(component.draftScope)) return true;
  return confirm('Existem alterações não salvas. Deseja sair e descartá-las?');
};
