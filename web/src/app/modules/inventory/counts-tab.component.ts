import { DatePipe, NgClass } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Contagem, ContagemEscopo, ContagemStatus } from '../../core/models';
import { DataService } from '../../core/services/data.service';
import { InventoryService } from '../../core/services/inventory.service';
import { ToastService } from '../../core/services/toast.service';
import { ModalComponent } from '../../shared/components/modal/modal.component';

const STATUS: Record<ContagemStatus, { rotulo: string; badge: string }> = {
  em_andamento: { rotulo: 'Em andamento', badge: 'proc' },
  aguardando_aprovacao: { rotulo: 'Aguardando aprovação', badge: 'pend' },
  aprovada: { rotulo: 'Aprovada', badge: 'auth' },
};

/** Physical count workflow: open → record counted quantities → approve (divergences become adjustments). */
@Component({
  selector: 'app-counts-tab',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, ModalComponent],
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Inventários (contagem física)</span>
        <button class="btn btn-primary btn-sm" type="button" (click)="abrirNova.set(true)">
          <i class="ti ti-plus"></i> Nova contagem
        </button>
      </div>
      <table class="data-table">
        <thead><tr><th>Aberta em</th><th>Escopo</th><th>Depósito</th><th>Itens contados</th><th>Situação</th><th></th></tr></thead>
        <tbody>
          @for (c of inventory.contagens(); track c.id) {
            <tr>
              <td class="muted">{{ c.iniciadaEm | date:'dd/MM HH:mm' }} · {{ c.iniciadaPor }}</td>
              <td>{{ c.escopo === 'total' ? 'Total' : 'Parcial — ' + c.grupo }}</td>
              <td class="small">{{ inventory.nomeDeposito(c.depositoId) }}</td>
              <td class="td-mono">{{ contados(c) }} / {{ c.linhas.length }}</td>
              <td>
                <span class="badge" [ngClass]="status(c.status).badge"><span class="badge-dot"></span>{{ status(c.status).rotulo }}</span>
              </td>
              <td class="td-actions">
                <button class="btn btn-ghost btn-sm" type="button" (click)="abrir(c)">{{ rotuloAcao(c) }}</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="empty">Nenhuma contagem aberta</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (abrirNova()) {
      <app-modal title="Nova contagem física" [width]="420" (closed)="abrirNova.set(false)">
        <div class="form-row">
          <div>
            <label class="form-label" for="ct-escopo">Escopo</label>
            <select id="ct-escopo" class="form-select" [ngModel]="escopo()" (ngModelChange)="escopo.set($event)">
              <option value="parcial">Parcial (por grupo de produtos)</option>
              <option value="total">Total</option>
            </select>
          </div>
        </div>
        @if (escopo() === 'parcial') {
          <div class="form-row">
            <div>
              <label class="form-label" for="ct-grupo">Grupo</label>
              <select id="ct-grupo" class="form-select" [ngModel]="grupo()" (ngModelChange)="grupo.set($event)">
                @for (g of grupos(); track g) { <option [value]="g">{{ g }}</option> }
              </select>
            </div>
          </div>
        }
        <div class="form-row">
          <div>
            <label class="form-label" for="ct-deposito">Depósito</label>
            <select id="ct-deposito" class="form-select" [ngModel]="depositoId()" (ngModelChange)="depositoId.set($event)">
              @for (d of inventory.depositos(); track d.id) { <option [value]="d.id">{{ d.nome }}</option> }
            </select>
          </div>
        </div>
        <div footer>
          <button class="btn btn-ghost" type="button" (click)="abrirNova.set(false)">Cancelar</button>
          <button class="btn btn-primary" type="button" (click)="iniciar()">Iniciar contagem</button>
        </div>
      </app-modal>
    }

    @if (selecionada(); as contagem) {
      <app-modal [title]="'Contagem ' + contagem.id" [width]="640" (closed)="selecionadaId.set(null)">
        <table class="data-table">
          <thead>
            <tr><th>Produto</th><th class="td-right">Sistema</th><th class="td-right">Contado</th><th class="td-right">Divergência</th></tr>
          </thead>
          <tbody>
            @for (l of contagem.linhas; track l.produtoId) {
              <tr>
                <td class="td-name">{{ l.produtoNome }}</td>
                <td class="td-mono td-right">{{ l.qtdSistema }}</td>
                <td class="td-right">
                  @if (contagem.status === 'em_andamento') {
                    <input class="form-input count-input" type="number" min="0" step="any" [attr.aria-label]="'Contagem de ' + l.produtoNome"
                           [ngModel]="rascunho()[l.produtoId] ?? l.qtdContada"
                           (ngModelChange)="registrar(l.produtoId, $event)" />
                  } @else {
                    <span class="td-mono">{{ l.qtdContada ?? '—' }}</span>
                  }
                </td>
                <td class="td-mono td-right" [class.text-danger]="inventory.divergencia(l) < 0" [class.text-warn]="inventory.divergencia(l) > 0">
                  {{ inventory.divergencia(l) > 0 ? '+' : '' }}{{ inventory.divergencia(l) }}
                </td>
              </tr>
            }
          </tbody>
        </table>
        @if (contagem.status === 'aguardando_aprovacao') {
          <div class="impact-note mt-14">
            <i class="ti ti-info-circle"></i>
            <span>Ao aprovar, cada divergência gera um ajuste de estoque com referência a esta contagem.</span>
          </div>
        }
        <div footer>
          <button class="btn btn-ghost" type="button" (click)="selecionadaId.set(null)">Fechar</button>
          @if (contagem.status === 'em_andamento') {
            <button class="btn btn-primary" type="button" (click)="enviar(contagem)">Salvar contagem</button>
          }
          @if (contagem.status === 'aguardando_aprovacao') {
            <button class="btn btn-primary" type="button" (click)="aprovar(contagem)">Aprovar e ajustar</button>
          }
        </div>
      </app-modal>
    }
  `,
  styles: [`
    .muted { font-size: 11px; color: var(--text3); }
    .small { font-size: 11px; }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
    .count-input { width: 90px; text-align: right; }
  `]
})
export class CountsTabComponent {
  private readonly data = inject(DataService);
  private readonly toast = inject(ToastService);
  readonly inventory = inject(InventoryService);

  readonly abrirNova = signal(false);
  readonly escopo = signal<ContagemEscopo>('parcial');
  readonly depositoId = signal(this.inventory.depositos()[0].id);
  readonly grupos = computed(() => [...new Set(this.data.produtos().map(p => p.grupo).filter((g): g is string => !!g))]);
  readonly grupo = signal(this.grupos()[0] ?? '');

  readonly selecionadaId = signal<string | null>(null);
  /** Quantities typed in the open sheet but not saved yet. */
  readonly rascunho = signal<Partial<Record<string, number>>>({});
  readonly selecionada = computed(() => this.inventory.contagens().find(c => c.id === this.selecionadaId()) ?? null);

  status(status: ContagemStatus): { rotulo: string; badge: string } {
    return STATUS[status];
  }

  contados(contagem: Contagem): number {
    return contagem.linhas.filter(l => l.qtdContada !== undefined).length;
  }

  rotuloAcao(contagem: Contagem): string {
    return { em_andamento: 'Contar', aguardando_aprovacao: 'Revisar', aprovada: 'Ver' }[contagem.status];
  }

  abrir(contagem: Contagem): void {
    this.rascunho.set({});
    this.selecionadaId.set(contagem.id);
  }

  private rascunhoPreenchido(): Record<string, number> {
    return Object.fromEntries(Object.entries(this.rascunho()).filter((par): par is [string, number] => par[1] !== undefined));
  }

  registrar(produtoId: string, valor: number | null): void {
    if (valor === null || valor === undefined || `${valor}` === '') return;
    this.rascunho.update(atual => ({ ...atual, [produtoId]: Number(valor) }));
  }

  iniciar(): void {
    const resultado = this.inventory.iniciarContagem({
      escopo: this.escopo(), grupo: this.escopo() === 'parcial' ? this.grupo() : undefined, depositoId: this.depositoId(),
    });
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success('Contagem iniciada');
    this.abrirNova.set(false);
  }

  enviar(contagem: Contagem): void {
    const resultado = this.inventory.enviarContagem(contagem.id, this.rascunhoPreenchido());
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.rascunho.set({});
    this.toast.success('Contagem salva');
  }

  aprovar(contagem: Contagem): void {
    const resultado = this.inventory.aprovarContagem(contagem.id);
    if (!resultado.ok) return this.toast.danger(resultado.erro);
    this.toast.success('Contagem aprovada — ajustes gerados');
    this.selecionadaId.set(null);
  }
}
