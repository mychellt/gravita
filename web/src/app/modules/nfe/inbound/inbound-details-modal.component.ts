import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { InboundConference, InboundNfeListItem, NfeService } from '../../../core/services/nfe.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';
import { formatCnpj, formatUnitValue } from './inbound-format';

/** Detalhes somente leitura de uma NF-e de entrada: dados da nota, fornecedor e itens (qualquer status). */
@Component({
  selector: 'app-inbound-details-modal',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ModalComponent, BrlPipe],
  styles: [`
    .summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px 16px; margin-bottom: 14px; }
    .summary dt { font-size: 11px; color: var(--text3); }
    .summary dd { margin: 2px 0 0; font-size: 13px; }
  `],
  template: `
    <app-modal title="Detalhes da NF-e de entrada" [width]="760" (closed)="closed.emit()">
      <dl class="summary">
        <div><dt>Número / Série</dt><dd class="td-mono">#{{ nfe.number }} / {{ nfe.series }}</dd></div>
        <div><dt>Emissão</dt><dd>{{ nfe.issuedAt | date:'dd/MM/yyyy HH:mm' }}</dd></div>
        <div><dt>Valor total</dt><dd>{{ nfe.totalValue | brl }}</dd></div>
        <div><dt>Status</dt><dd><app-badge [value]="nfe.status" /></dd></div>
        <div><dt>Fornecedor</dt><dd>{{ nfe.supplierName }}</dd></div>
        @if (conferencia(); as c) {
          <div><dt>CNPJ do fornecedor</dt><dd class="td-mono">{{ formatarCnpj(c.supplierDocument) }}</dd></div>
        }
      </dl>
      @if (carregamento() === 'loading') { <div class="field-hint">Carregando os itens da nota…</div> }
      @if (carregamento() === 'error') {
        <div class="field-error" role="alert">
          Não foi possível carregar os itens da nota.
          <button class="btn btn-ghost btn-sm" type="button" (click)="carregar()">Tentar novamente</button>
        </div>
      }
      @if (conferencia(); as c) {
        <table class="data-table">
          <thead>
            <tr>
              <th>Cód. fornecedor</th><th>Descrição</th><th>NCM</th><th>Un.</th>
              <th class="td-right">Qtd.</th><th class="td-right">Valor unit.</th><th class="td-right">Total</th><th>Produto</th>
            </tr>
          </thead>
          <tbody>
            @for (item of c.items; track item.index) {
              <tr>
                <td class="td-mono">{{ item.supplierProductCode }}</td>
                <td>{{ item.description }}</td>
                <td class="td-mono">{{ item.ncm }}</td>
                <td>{{ item.unit }}</td>
                <td class="td-right">{{ item.quantity }}</td>
                <td class="td-right">{{ unitario(item.unitValue) }}</td>
                <td class="td-amount">{{ item.quantity * item.unitValue | brl }}</td>
                <td>{{ item.resolved ? item.productLabel : 'Sem vínculo' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="8" style="text-align:center;color:var(--text3)">A nota não tem itens.</td></tr>
            }
          </tbody>
        </table>
      }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Fechar</button>
      </div>
    </app-modal>
  `
})
export class InboundDetailsModalComponent implements OnInit {
  private readonly service = inject(NfeService);

  @Input({ required: true }) nfe!: InboundNfeListItem;
  @Output() closed = new EventEmitter<void>();

  readonly conferencia = signal<InboundConference | null>(null);
  readonly carregamento = signal<'loading' | 'ready' | 'error'>('loading');
  readonly formatarCnpj = formatCnpj;
  readonly unitario = formatUnitValue;

  ngOnInit(): void {
    void this.carregar();
  }

  /** GET /api/nfe/inbound/{id}/conference — somente leitura; nada é gravado. */
  async carregar(): Promise<void> {
    this.carregamento.set('loading');
    try {
      this.conferencia.set(await this.service.loadInboundConference(this.nfe.id));
      this.carregamento.set('ready');
    } catch {
      this.carregamento.set('error');
    }
  }
}
