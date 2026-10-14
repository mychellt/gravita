import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import {
  TransportModalityCode, TransportModalityOption, TransportModalityService, transportModalityFailureDetail,
} from '../../../core/services/transport-modality.service';
import { ADMINISTRATOR_PROFILE } from '../../../core/user-display';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

export const MODALITY_CODES: { code: TransportModalityCode; label: string }[] = [
  { code: 'CIF', label: 'CIF — frete por conta do emitente' },
  { code: 'FOB', label: 'FOB — frete por conta do destinatário' },
  { code: 'NONE', label: 'NONE — sem transporte' },
];

/**
 * Configurações › Modalidades de frete: cadastro das opções do select Transporte > Modalidade da NF-e.
 * Todos os perfis veem a lista; só o Administrador cadastra, edita e desativa. Excluir é desativar: a linha fica,
 * inativa, e as NF-e já emitidas não mudam.
 */
@Component({
  selector: 'app-transport-modalities',
  standalone: true,
  imports: [BadgeComponent, PageHeaderComponent],
  styleUrl: './transport-modalities.component.scss',
  templateUrl: './transport-modalities.component.html'
})
export class TransportModalitiesComponent implements OnInit {
  private readonly service = inject(TransportModalityService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly codes = MODALITY_CODES;
  readonly status = this.service.managedStatus;
  readonly modalities = this.service.managed;
  readonly canEdit = computed(() => this.auth.currentUser()?.profile === ADMINISTRATOR_PROFILE);

  // ── formulário de cadastro / edição ──
  readonly formOpen = signal(false);
  /** Id da linha em edição; `null` = cadastro de uma nova modalidade. */
  readonly editingId = signal<string | null>(null);
  readonly name = signal('');
  readonly code = signal<TransportModalityCode | ''>('');
  readonly active = signal(true);
  readonly submitting = signal(false);
  readonly formError = signal('');

  readonly canSubmit = computed(() => !this.submitting() && this.name().trim() !== '' && this.code() !== '');

  ngOnInit() {
    void this.service.loadAll();
  }

  reload() {
    void this.service.loadAll();
  }

  openCreate() {
    this.resetForm();
    this.formOpen.set(true);
  }

  openEdit(row: TransportModalityOption) {
    this.resetForm();
    this.editingId.set(row.id);
    this.name.set(row.name);
    this.code.set(row.code);
    this.active.set(row.active);
    this.formOpen.set(true);
  }

  closeForm() {
    this.formOpen.set(false);
    this.resetForm();
  }

  async submit(event?: Event) {
    event?.preventDefault();
    const code = this.code();
    if (!this.canSubmit() || code === '') return;

    this.formError.set('');
    this.submitting.set(true);
    const id = this.editingId();
    try {
      if (id === null) {
        await this.service.create({ name: this.name().trim(), code });
        this.toast.success('Modalidade cadastrada.');
      } else {
        await this.service.update(id, { name: this.name().trim(), code, active: this.active() });
        this.toast.success('Modalidade atualizada.');
      }
      this.closeForm();
    } catch (error) {
      const detail = transportModalityFailureDetail(error);
      this.formError.set(detail ? `Não foi possível salvar: ${detail}` : 'Não foi possível salvar a modalidade agora. Tente novamente.');
    } finally {
      this.submitting.set(false);
    }
  }

  async deactivate(row: TransportModalityOption) {
    if (!confirm(`Desativar "${row.name}"? Ela deixa de ser oferecida nas novas NF-e; as notas já emitidas não mudam.`)) return;
    try {
      await this.service.deactivate(row.id);
      this.toast.success(`"${row.name}" foi desativada.`);
    } catch (error) {
      const detail = transportModalityFailureDetail(error);
      this.toast.danger(detail ? `Não foi possível desativar: ${detail}` : 'Não foi possível desativar a modalidade.');
    }
  }

  async reactivate(row: TransportModalityOption) {
    try {
      await this.service.update(row.id, { name: row.name, code: row.code, active: true });
      this.toast.success(`"${row.name}" foi reativada.`);
    } catch (error) {
      const detail = transportModalityFailureDetail(error);
      this.toast.danger(detail ? `Não foi possível reativar: ${detail}` : 'Não foi possível reativar a modalidade.');
    }
  }

  private resetForm() {
    this.editingId.set(null);
    this.name.set('');
    this.code.set('');
    this.active.set(true);
    this.formError.set('');
  }
}
