import { Component, EventEmitter, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';
import { ToastService } from '../../core/services/toast.service';
import { ModalComponent } from '../../shared/components/modal/modal.component';

type Sentido = 'entrada' | 'saida';

@Component({
  selector: 'app-adjust-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent],
  template: `
    <app-modal title="Ajuste manual de estoque" (closed)="closed.emit()">
      <div class="impact-note">
        <i class="ti ti-info-circle"></i>
        <span>O ajuste gera um lançamento contábil automático e fica registrado no histórico de movimentações.</span>
      </div>
      <div class="form-row cols-2">
        <div>
          <label class="form-label" for="adj-produto">Produto</label>
          <select id="adj-produto" class="form-select" [ngModel]="produtoId()" (ngModelChange)="produtoId.set($event)">
            @for (p of data.produtos(); track p.id) {
              @if (p.tipo !== 'servico') { <option [value]="p.id">{{ p.descricao }}</option> }
            }
          </select>
        </div>
        <div>
          <label class="form-label" for="adj-deposito">Depósito</label>
          <select id="adj-deposito" class="form-select" [ngModel]="depositoId()" (ngModelChange)="depositoId.set($event)">
            @for (d of inventory.depositos(); track d.id) { <option [value]="d.id">{{ d.nome }}</option> }
          </select>
        </div>
      </div>
      <div class="form-row cols-2">
        <div>
          <label class="form-label" for="adj-sentido">Tipo de ajuste</label>
          <select id="adj-sentido" class="form-select" [ngModel]="sentido()" (ngModelChange)="sentido.set($event)">
            <option value="entrada">Positivo (acrescenta)</option>
            <option value="saida">Negativo (retira)</option>
          </select>
        </div>
        <div>
          <label class="form-label" for="adj-qtd">Quantidade</label>
          <input id="adj-qtd" class="form-input" type="number" min="0" step="any"
                 [ngModel]="quantidade()" (ngModelChange)="quantidade.set($event)" />
          <div class="field-hint">Saldo atual neste depósito: {{ saldoAtual() }} · depois: {{ saldoDepois() }}</div>
        </div>
      </div>
      <div class="form-row">
        <div>
          <label class="form-label" for="adj-just">Justificativa (obrigatória)</label>
          <textarea id="adj-just" class="form-textarea" rows="3" placeholder="Ex.: avaria, quebra, erro de lançamento…"
                    [class.invalid]="tentouSalvar() && !justificativa().trim()"
                    [ngModel]="justificativa()" (ngModelChange)="justificativa.set($event)"></textarea>
          @if (tentouSalvar() && !justificativa().trim()) {
            <div class="field-error">Informe o motivo do ajuste.</div>
          }
        </div>
      </div>
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" (click)="salvar()">Registrar ajuste</button>
      </div>
    </app-modal>
  `
})
export class AdjustModalComponent {
  readonly data = inject(DataService);
  readonly inventory = inject(InventoryService);
  private readonly toast = inject(ToastService);

  @Output() closed = new EventEmitter<void>();

  readonly produtoId = signal(this.data.produtos().find(p => p.tipo !== 'servico')?.id ?? '');
  readonly depositoId = signal(this.inventory.depositos()[0].id);
  readonly sentido = signal<Sentido>('entrada');
  readonly quantidade = signal<number | null>(null);
  readonly justificativa = signal('');
  readonly tentouSalvar = signal(false);

  readonly saldoAtual = computed(() =>
    this.inventory.saldosDoProduto(this.produtoId()).find(s => s.depositoId === this.depositoId())?.fisico ?? 0);
  readonly saldoDepois = computed(() => this.saldoAtual() + this.variacao());

  private variacao(): number {
    const quantidade = Number(this.quantidade()) || 0;
    return this.sentido() === 'entrada' ? quantidade : -quantidade;
  }

  salvar(): void {
    this.tentouSalvar.set(true);
    const resultado = this.inventory.ajustar({
      produtoId: this.produtoId(), depositoId: this.depositoId(),
      variacao: this.variacao(), justificativa: this.justificativa(),
    });
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success('Ajuste registrado e lançamento contábil gerado');
    this.closed.emit();
  }
}
