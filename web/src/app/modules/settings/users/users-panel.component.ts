import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  UserService, UserStatus, UserSummary, isDuplicateEmail, userFailureDetail,
} from '../../../core/services/user.service';
import { ADMINISTRATOR_PROFILE, profileLabel } from '../../../core/user-display';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const SELF_DEACTIVATION_HINT = 'Você não pode inativar sua própria conta';
export const DUPLICATE_EMAIL_MESSAGE = 'Este e-mail já está cadastrado.';

const STATUS_BADGES: Record<UserStatus, string> = {
  ACTIVE: 'ativo',
  INACTIVE: 'inativo',
  PENDING_ACTIVATION: 'aguardando_ativacao',
};

/** Usuários da empresa (Configurações › Usuários): lista, cadastro e inativação. Só o Administrador chega aqui. */
@Component({
  selector: 'app-users-panel',
  standalone: true,
  imports: [BadgeComponent],
  styleUrl: './users-panel.component.scss',
  templateUrl: './users-panel.component.html'
})
export class UsersPanelComponent {
  private readonly service = inject(UserService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly selfDeactivationHint = SELF_DEACTIVATION_HINT;
  readonly badge = (status: UserStatus) => STATUS_BADGES[status];
  readonly label = profileLabel;

  readonly status = this.service.status;
  readonly profiles = this.service.profiles;
  readonly isAdministrator = computed(() => this.auth.currentUser()?.profile === ADMINISTRATOR_PROFILE);

  readonly users = computed(() => this.service.users());

  // ── formulário de cadastro ──
  readonly formOpen = signal(false);
  readonly name = signal('');
  readonly email = signal('');
  readonly password = signal('');
  readonly profileId = signal('');
  readonly submitting = signal(false);
  /** Erro do e-mail devolvido pelo servidor; some assim que o e-mail é editado. */
  readonly emailError = signal('');
  readonly formError = signal('');

  readonly emailInvalid = computed(() => this.email().trim() !== '' && !EMAIL.test(this.email().trim()));
  readonly canSubmit = computed(() =>
    !this.submitting() && this.name().trim() !== '' && EMAIL.test(this.email().trim())
    && this.password() !== '' && this.profileId() !== '');

  constructor() {
    // O perfil do usuário chega de forma assíncrona: a lista só é pedida quando se sabe que é o Administrador.
    effect(() => {
      if (this.isAdministrator() && this.status() === 'idle') untracked(() => void this.service.load());
    });
  }

  reload() {
    void this.service.load();
  }

  openForm() {
    this.formOpen.set(true);
  }

  closeForm() {
    this.formOpen.set(false);
    this.name.set(''); this.email.set(''); this.password.set(''); this.profileId.set('');
    this.emailError.set(''); this.formError.set('');
  }

  onEmailInput(value: string) {
    this.email.set(value);
    this.emailError.set('');
  }

  async submit(event?: Event) {
    event?.preventDefault();
    if (!this.canSubmit()) return;

    this.emailError.set(''); this.formError.set('');
    this.submitting.set(true);
    try {
      await this.service.register({
        name: this.name().trim(), email: this.email().trim(), password: this.password(), profileId: this.profileId(),
      });
      this.toast.success('Usuário cadastrado.');
      this.closeForm();
    } catch (error) {
      if (isDuplicateEmail(error)) {
        this.emailError.set(DUPLICATE_EMAIL_MESSAGE);
      } else {
        const detail = userFailureDetail(error);
        this.formError.set(detail ? `Não foi possível cadastrar: ${detail}` : 'Não foi possível cadastrar o usuário agora. Tente novamente.');
      }
    } finally {
      this.submitting.set(false);
    }
  }

  // ── inativação ──
  /** O Administrador não pode se trancar para fora: a linha do próprio usuário não oferece "Inativar". */
  isSelf(user: UserSummary): boolean {
    const me = this.auth.currentUser()?.email;
    return !!me && me.toLowerCase() === user.email.toLowerCase();
  }

  async deactivate(user: UserSummary) {
    if (this.isSelf(user)) return;
    if (!confirm(`Inativar ${user.name}? Esta pessoa deixará de conseguir entrar no sistema.`)) return;
    try {
      await this.service.deactivate(user.id);
      this.toast.success(`${user.name} foi inativado.`);
    } catch (error) {
      const detail = userFailureDetail(error);
      this.toast.danger(detail ? `Não foi possível inativar: ${detail}` : 'Não foi possível inativar o usuário.');
    }
  }
}
