import { DatePipe, NgClass } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LoadStatus } from '../../core/services/customer.service';
import { InventoryChangesService } from '../../core/services/inventory-changes.service';
import {
  CountScope, CountStatus, InventoryApiService, Page, PhysicalCountItem, PhysicalCountLine, inventoryFailureDetail,
  productLabel
} from '../../core/services/inventory-api.service';
import { ToastService } from '../../core/services/toast.service';
import { LoadStateComponent } from '../../shared/components/load-state/load-state.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { PagerComponent } from '../../shared/components/pager/pager.component';

const PAGE_SIZE = 20;

const STATUS: Record<CountStatus, { rotulo: string; badge: string }> = {
  IN_PROGRESS: { rotulo: 'Em andamento', badge: 'proc' },
  PENDING_APPROVAL: { rotulo: 'Aguardando aprovação', badge: 'pend' },
  APPROVED: { rotulo: 'Aprovada', badge: 'auth' },
};

/** Physical count workflow: open → record counted quantities → approve (divergences become adjustments). */
@Component({
  selector: 'app-counts-tab',
  standalone: true,
  imports: [DatePipe, NgClass, FormsModule, LoadStateComponent, ModalComponent, PagerComponent],
  template: `
    <div class="card">
      <div class="card-head">
        <span class="card-title">Inventários (contagem física)</span>
        <button class="btn btn-primary btn-sm" type="button" (click)="abrirNova.set(true)">
          <i class="ti ti-plus"></i> Nova contagem
        </button>
      </div>
      @if (status() !== 'ready') {
        <app-load-state [status]="status()" loadingText="Carregando contagens…"
                        errorText="Não foi possível carregar as contagens." (retry)="load(pagina().page)" />
      } @else {
        <table class="data-table">
          <thead><tr><th>Aberta em</th><th>Escopo</th><th>Itens contados</th><th>Situação</th><th></th></tr></thead>
          <tbody>
            @for (c of pagina().content; track c.id) {
              <tr>
                <td class="muted">{{ c.startedAt | date:'dd/MM HH:mm' }} · {{ c.startedBy.slice(0, 8) }}</td>
                <td>{{ c.scope === 'TOTAL' ? 'Total' : 'Parcial — ' + c.productGroupId }}</td>
                <td class="td-mono">{{ contados(c) }} / {{ c.lines.length }}</td>
                <td>
                  <span class="badge" [ngClass]="situacao(c.status).badge"><span class="badge-dot"></span>{{ situacao(c.status).rotulo }}</span>
                </td>
                <td class="td-actions">
                  <button class="btn btn-ghost btn-sm" type="button" (click)="abrir(c)">{{ rotuloAcao(c) }}</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="empty">Nenhuma contagem aberta</td></tr>
            }
          </tbody>
        </table>
        <app-pager [page]="pagina().page" [totalPages]="pagina().totalPages" [totalElements]="pagina().totalElements"
                   (pageChange)="load($event)" />
      }
    </div>

    @if (abrirNova()) {
      <app-modal title="Nova contagem física" [width]="420" (closed)="fecharNova()">
        <div class="form-row">
          <div>
            <label class="form-label" for="ct-escopo">Escopo</label>
            <select id="ct-escopo" class="form-select" [ngModel]="escopo()" (ngModelChange)="escopo.set($event)">
              <option value="PARTIAL_BY_GROUP">Parcial (por grupo de produtos)</option>
              <option value="TOTAL">Total</option>
            </select>
          </div>
        </div>
        @if (escopo() === 'PARTIAL_BY_GROUP') {
          <div class="form-row">
            <div>
              <label class="form-label" for="ct-grupo">Grupo de produtos</label>
              <input id="ct-grupo" class="form-input" type="text" [class.invalid]="tentouIniciar() && !grupo().trim()"
                     [ngModel]="grupo()" (ngModelChange)="grupo.set($event)" />
              @if (tentouIniciar() && !grupo().trim()) {
                <div class="field-error">Informe o grupo de produtos da contagem parcial.</div>
              }
            </div>
          </div>
        }
        <div footer>
          <button class="btn btn-ghost" type="button" (click)="fecharNova()">Cancelar</button>
          <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="iniciar()">
            {{ enviando() ? 'Iniciando…' : 'Iniciar contagem' }}
          </button>
        </div>
      </app-modal>
    }

    @if (selecionada(); as contagem) {
      <app-modal [title]="'Contagem ' + contagem.id.slice(0, 8)" [width]="640" (closed)="selecionadaId.set(null)">
        <table class="data-table">
          <thead>
            <tr><th>Produto</th><th class="td-right">Sistema</th><th class="td-right">Contado</th><th class="td-right">Divergência</th></tr>
          </thead>
          <tbody>
            @for (l of contagem.lines; track l.productId) {
              <tr>
                <td class="td-name">{{ rotulo(l) }}</td>
                <td class="td-mono td-right">{{ l.systemQuantity }}</td>
                <td class="td-right">
                  @if (contagem.status === 'IN_PROGRESS') {
                    <input class="form-input count-input" type="number" min="0" step="any" [attr.aria-label]="'Contagem de ' + rotulo(l)"
                           [ngModel]="rascunho()[l.productId] ?? l.countedQuantity"
                           (ngModelChange)="registrar(l.productId, $event)" />
                  } @else {
                    <span class="td-mono">{{ l.countedQuantity ?? '—' }}</span>
                  }
                </td>
                <td class="td-mono td-right" [class.text-danger]="(l.divergence ?? 0) < 0" [class.text-warn]="(l.divergence ?? 0) > 0">
                  {{ l.divergence === null ? '—' : (l.divergence > 0 ? '+' : '') + l.divergence }}
                </td>
              </tr>
            }
          </tbody>
        </table>
        @if (contagem.status === 'PENDING_APPROVAL') {
          <div class="impact-note mt-14">
            <i class="ti ti-info-circle"></i>
            <span>Ao aprovar, cada divergência gera um ajuste de estoque com referência a esta contagem.</span>
          </div>
        }
        <div footer>
          <button class="btn btn-ghost" type="button" (click)="selecionadaId.set(null)">Fechar</button>
          @if (contagem.status === 'IN_PROGRESS') {
            <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="salvar(contagem)">
              {{ enviando() ? 'Salvando…' : 'Salvar contagem' }}
            </button>
          }
          @if (contagem.status === 'PENDING_APPROVAL') {
            <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="aprovar(contagem)">
              {{ enviando() ? 'Aprovando…' : 'Aprovar e ajustar' }}
            </button>
          }
        </div>
      </app-modal>
    }
  `,
  styles: [`
    .muted { font-size: 11px; color: var(--text3); }
    .empty { text-align: center; padding: 24px; color: var(--text3); }
    .count-input { width: 90px; text-align: right; }
  `]
})
export class CountsTabComponent implements OnInit {
  private readonly api = inject(InventoryApiService);
  private readonly changes = inject(InventoryChangesService);
  private readonly toast = inject(ToastService);
  private loads = 0;

  readonly status = signal<LoadStatus>('loading');
  readonly pagina = signal<Page<PhysicalCountItem>>({ content: [], page: 0, size: PAGE_SIZE, totalElements: 0, totalPages: 0 });

  readonly abrirNova = signal(false);
  readonly escopo = signal<CountScope>('PARTIAL_BY_GROUP');
  readonly grupo = signal('');
  readonly tentouIniciar = signal(false);
  /** Uma escrita em andamento (iniciar, salvar ou aprovar): bloqueia o botão para não duplicar a chamada. */
  readonly enviando = signal(false);

  readonly selecionadaId = signal<string | null>(null);
  /** Quantities typed in the open sheet but not saved yet. */
  readonly rascunho = signal<Partial<Record<string, number>>>({});
  readonly selecionada = computed(() => this.pagina().content.find(c => c.id === this.selecionadaId()) ?? null);

  ngOnInit(): void {
    void this.load(0);
  }

  /** GET /api/inventory/counts — nunca rejeita: o resultado fica em `status`. */
  async load(page: number): Promise<void> {
    const load = ++this.loads;
    this.status.set('loading');
    try {
      const result = await this.api.counts(page, PAGE_SIZE);
      if (load !== this.loads) return;
      this.pagina.set(result);
      this.status.set('ready');
    } catch {
      if (load === this.loads) this.status.set('error');
    }
  }

  situacao(status: CountStatus): { rotulo: string; badge: string } {
    return STATUS[status];
  }

  rotulo(linha: PhysicalCountLine): string {
    return productLabel(linha.productId, linha.productCode);
  }

  contados(contagem: PhysicalCountItem): number {
    return contagem.lines.filter(l => l.countedQuantity !== null).length;
  }

  rotuloAcao(contagem: PhysicalCountItem): string {
    return { IN_PROGRESS: 'Contar', PENDING_APPROVAL: 'Revisar', APPROVED: 'Ver' }[contagem.status];
  }

  abrir(contagem: PhysicalCountItem): void {
    this.rascunho.set({});
    this.selecionadaId.set(contagem.id);
  }

  fecharNova(): void {
    this.abrirNova.set(false);
    this.tentouIniciar.set(false);
  }

  registrar(produtoId: string, valor: number | null): void {
    if (valor === null || valor === undefined || `${valor}` === '') return;
    this.rascunho.update(atual => ({ ...atual, [produtoId]: Number(valor) }));
  }

  /** POST /api/inventory/counts */
  async iniciar(): Promise<void> {
    this.tentouIniciar.set(true);
    const parcial = this.escopo() === 'PARTIAL_BY_GROUP';
    if (parcial && !this.grupo().trim()) return;
    await this.executar(
      () => this.api.startCount(this.escopo(), parcial ? this.grupo().trim() : null),
      'Não foi possível iniciar a contagem.',
      () => {
        this.toast.success('Contagem iniciada');
        this.fecharNova();
      });
  }

  /** POST /api/inventory/counts/{id}/submit — manda só as quantidades digitadas; as já salvas ficam como estão. */
  async salvar(contagem: PhysicalCountItem): Promise<void> {
    const countedLines = Object.entries(this.rascunho())
      .filter((par): par is [string, number] => par[1] !== undefined)
      .map(([productId, countedQuantity]) => ({ productId, countedQuantity }));
    if (!countedLines.length) return this.toast.danger('Informe ao menos uma quantidade contada.');
    await this.executar(
      () => this.api.submitCount(contagem.id, countedLines),
      'Não foi possível salvar a contagem.',
      () => {
        this.rascunho.set({});
        this.toast.success('Contagem salva');
      });
  }

  /** POST /api/inventory/counts/{id}/approve — os ajustes gerados mudam saldos e movimentações no servidor. */
  async aprovar(contagem: PhysicalCountItem): Promise<void> {
    await this.executar(
      () => this.api.approveCount(contagem.id),
      'Não foi possível aprovar a contagem.',
      () => {
        this.toast.success('Contagem aprovada — ajustes gerados');
        this.selecionadaId.set(null);
        this.changes.stockChanged();
      });
  }

  /** Roda uma escrita; só depois da resposta de sucesso aplica `aoConcluir` e busca a lista de novo. */
  private async executar(escrita: () => Promise<unknown>, falha: string, aoConcluir: () => void): Promise<void> {
    if (this.enviando()) return;
    this.enviando.set(true);
    try {
      await escrita();
    } catch (error) {
      this.enviando.set(false);
      return this.toast.danger(inventoryFailureDetail(error) || falha);
    }
    this.enviando.set(false);
    aoConcluir();
    void this.load(this.pagina().page);
  }
}
