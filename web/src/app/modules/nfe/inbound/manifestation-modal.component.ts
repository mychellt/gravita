import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ManifestationType, NfeService, nfeFailureDetail } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { ACCESS_KEY_PATTERN, onlyDigits } from './inbound-format';

const TYPES: { value: ManifestationType; label: string }[] = [
  { value: 'CONFIRMED', label: 'Confirmação da operação' },
  { value: 'UNKNOWN', label: 'Desconhecimento da operação' },
  { value: 'OPERATION_NOT_PERFORMED', label: 'Operação não realizada' },
];

@Component({
  selector: 'app-manifestation-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent],
  template: `
    <app-modal title="Manifestar destinatário" (closed)="closed.emit()">
      <div class="form-row">
        <div>
          <label class="form-label" for="man-chave">Chave de acesso (44 dígitos)</label>
          <input id="man-chave" class="form-input td-mono" type="text" inputmode="numeric" maxlength="60"
                 [class.invalid]="tentouEnviar() && !chaveValida()"
                 [ngModel]="chave()" (ngModelChange)="chave.set($event)" />
          @if (tentouEnviar() && !chaveValida()) { <div class="field-error">A chave de acesso deve ter 44 dígitos.</div> }
        </div>
      </div>
      <div class="form-row">
        <div>
          <label class="form-label" for="man-tipo">Manifestação</label>
          <select id="man-tipo" class="form-select" [ngModel]="tipo()" (ngModelChange)="tipo.set($event)">
            @for (t of tipos; track t.value) { <option [value]="t.value">{{ t.label }}</option> }
          </select>
        </div>
      </div>
      @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="enviar()">
          {{ enviando() ? 'Enviando…' : 'Manifestar' }}
        </button>
      </div>
    </app-modal>
  `
})
export class ManifestationModalComponent {
  private readonly nfe = inject(NfeService);
  private readonly toast = inject(ToastService);

  @Output() closed = new EventEmitter<void>();

  readonly tipos = TYPES;
  readonly chave = signal('');
  readonly tipo = signal<ManifestationType>('CONFIRMED');
  readonly tentouEnviar = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal('');

  chaveValida(): boolean {
    return ACCESS_KEY_PATTERN.test(onlyDigits(this.chave()));
  }

  /** POST /api/nfe/inbound/manifestation; mantém o modal aberto com a mensagem do backend nas falhas. */
  async enviar(): Promise<void> {
    this.tentouEnviar.set(true);
    if (!this.chaveValida() || this.enviando()) return;
    this.enviando.set(true);
    this.erro.set('');
    try {
      await this.nfe.manifestInbound(onlyDigits(this.chave()), this.tipo());
      this.toast.success('Manifestação registrada');
      this.closed.emit();
    } catch (error) {
      this.erro.set((await nfeFailureDetail(error)) || 'Não foi possível registrar a manifestação.');
      this.enviando.set(false);
    }
  }
}
