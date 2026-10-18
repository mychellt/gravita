import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { InboundNfeListItem, NfeListItem, NfeService, nfeFailureDetail } from '../../core/services/nfe.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';
import { ConfirmReceiptModalComponent } from './inbound/confirm-receipt-modal.component';
import { InboundDetailsModalComponent } from './inbound/inbound-details-modal.component';
import { ImportXmlModalComponent } from './inbound/import-xml-modal.component';
import { ManifestationModalComponent } from './inbound/manifestation-modal.component';
import { ManualEntryModalComponent } from './inbound/manual-entry-modal.component';

type NfeTab = 'emitidas' | 'entradas' | 'canceladas' | 'sped';
type InboundDialog = 'import' | 'manual' | 'manifestation';

const sameDay = (a: Date, b: Date) => a.toDateString() === b.toDateString();
const sameMonth = (a: Date, b: Date) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth();
const sum = (list: NfeListItem[]) => list.reduce((total, n) => total + n.totalValue, 0);

@Component({
  selector: 'app-nfe',
  standalone: true,
  imports: [
    RouterLink, DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe,
    ImportXmlModalComponent, ManualEntryModalComponent, ManifestationModalComponent, ConfirmReceiptModalComponent,
    InboundDetailsModalComponent,
  ],
  styleUrl: './nfe.component.scss',
  templateUrl: './nfe.component.html'
})
export class NfeComponent {
  private readonly nfeService = inject(NfeService);
  private readonly toast = inject(ToastService);

  constructor() {
    void this.nfeService.load();
  }

  activeTab = signal<NfeTab>('emitidas');
  searchQuery = signal('');
  /** Id da NF-e com uma ação em andamento (cancelar/baixar), para não disparar a mesma ação duas vezes. */
  busyId = signal<string | null>(null);
  /** Modal de entrada aberto (importar, lançar, manifestar), se algum. */
  inboundDialog = signal<InboundDialog | null>(null);
  /** Nota de entrada em conferência, se o modal de conferência está aberto. */
  conferring = signal<InboundNfeListItem | null>(null);
  detailing = signal<InboundNfeListItem | null>(null);

  /** Estado de carga da lista da aba ativa: Emitidas/Canceladas vêm de /api/nfe, Entradas de /api/nfe/inbound. */
  readonly loadStatus = computed(() =>
    this.activeTab() === 'entradas' ? this.nfeService.inboundLoadStatus() : this.nfeService.status());

  readonly filtered = computed(() => {
    const q = this.searchQuery().toLowerCase();
    const tab = this.activeTab();
    return this.nfeService.nfes().filter(n => {
      const matchTab = tab === 'emitidas' ? n.status !== 'CANCELLED' : tab === 'canceladas' && n.status === 'CANCELLED';
      const matchQ = !q ||
        n.recipientName.toLowerCase().includes(q) ||
        String(n.documentNumber ?? '').includes(q) ||
        n.cfop.includes(q);
      return matchTab && matchQ;
    });
  });

  readonly filteredInbound = computed(() => {
    const q = this.searchQuery().toLowerCase();
    return this.nfeService.inboundNfes().filter(n =>
      !q || n.supplierName.toLowerCase().includes(q) || n.number.includes(q));
  });

  readonly kpis = computed(() => {
    const now = new Date();
    const issued = this.nfeService.nfes();
    const emitted = (n: NfeListItem) => new Date(n.emittedAt);
    const authorizedToday = issued.filter(n => n.status === 'AUTHORIZED' && sameDay(emitted(n), now));
    const pending = issued.filter(n => n.status === 'QUEUED' || n.status === 'SENT');
    const cancelledThisMonth = issued.filter(n => n.status === 'CANCELLED' && sameMonth(new Date(n.cancelledAt ?? n.emittedAt), now));
    const issuedThisMonth = issued.filter(n => n.status !== 'CANCELLED' && sameMonth(emitted(n), now));
    return {
      authorizedToday: { count: authorizedToday.length, total: sum(authorizedToday) },
      pending: { count: pending.length },
      cancelledThisMonth: { count: cancelledThisMonth.length, total: sum(cancelledThisMonth) },
      issuedThisMonth: { count: issuedThisMonth.length, total: sum(issuedThisMonth) },
    };
  });

  reload() { void this.nfeService.load(); }

  async cancelNfe(nfe: NfeListItem) {
    const justification = this.askJustification(nfe);
    if (justification === null) return;
    if (!justification.trim()) {
      this.toast.danger('Informe a justificativa do cancelamento.');
      return;
    }
    this.busyId.set(nfe.id);
    try {
      await this.nfeService.cancel(nfe.id, justification.trim());
      this.toast.warn('NF-e cancelada com sucesso.');
    } catch (error) {
      this.toast.danger((await nfeFailureDetail(error)) || 'Não foi possível cancelar a NF-e.');
    } finally {
      this.busyId.set(null);
    }
  }

  downloadDanfe(nfe: NfeListItem) {
    return this.download(nfe, 'pdf', 'DANFE', () => this.nfeService.danfe(nfe.id));
  }

  downloadXml(nfe: NfeListItem) {
    return this.download(nfe, 'xml', 'XML', () => this.nfeService.xml(nfe.id));
  }

  /** Pede a justificativa do cancelamento; `null` quando o usuário desiste. */
  protected askJustification(nfe: NfeListItem): string | null {
    return window.prompt(`Justificativa para cancelar a NF-e #${nfe.documentNumber}:`);
  }

  /** Entrega o arquivo ao navegador. */
  protected saveFile(blob: Blob, fileName: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
  }

  private async download(nfe: NfeListItem, extension: string, label: string, fetchFile: () => Promise<Blob>) {
    this.busyId.set(nfe.id);
    try {
      this.saveFile(await fetchFile(), `nfe-${nfe.documentSeries}-${nfe.documentNumber}.${extension}`);
    } catch (error) {
      this.toast.danger((await nfeFailureDetail(error)) || `Não foi possível baixar o ${label} da NF-e #${nfe.documentNumber}.`);
    } finally {
      this.busyId.set(null);
    }
  }
}
