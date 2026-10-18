import { Component, computed, inject } from '@angular/core';
import { DatePipe, SlicePipe } from '@angular/common';
import { NfseListItem, NfseService } from '../../core/services/nfse.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

const sameMonth = (a: Date, b: Date) => a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth();
const sum = (list: NfseListItem[], pick: (n: NfseListItem) => number) => list.reduce((total, n) => total + pick(n), 0);

@Component({
  selector: 'app-nfse',
  standalone: true,
  imports: [DatePipe, SlicePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './nfse.component.scss',
  templateUrl: './nfse.component.html'
})
export class NfseComponent {
  private readonly service = inject(NfseService);
  private readonly toast = inject(ToastService);

  constructor() {
    void this.service.load();
  }

  readonly nfses = this.service.nfses;
  readonly loadStatus = this.service.status;

  /** Totais do mês corrente; canceladas ficam de fora. */
  readonly kpis = computed(() => {
    const now = new Date();
    const month = this.nfses().filter(n => n.status !== 'CANCELLED' && sameMonth(new Date(n.issuedAt), now));
    const municipalities = new Set(this.nfses().map(n => n.municipalityIbgeCode));
    return {
      count: month.length,
      serviceTotal: sum(month, n => n.serviceAmount),
      issTotal: sum(month, n => n.issAmount),
      withheldTotal: sum(month, n => n.withheldAmount),
      municipalities: municipalities.size,
    };
  });

  reload() { void this.service.load(); }
  emitirRps() { this.toast.success('RPS emitido com sucesso!'); }
}
