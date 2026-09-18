import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-nfe',
  standalone: true,
  imports: [RouterLink, DatePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './nfe.component.scss',
  templateUrl: './nfe.component.html'
})
export class NfeComponent {
  constructor(public data: DataService, private toast: ToastService) {}

  activeTab = signal<'emitidas' | 'entradas' | 'canceladas' | 'sped'>('emitidas');
  searchQuery = signal('');

  readonly filtered = computed(() => {
    const q = this.searchQuery().toLowerCase();
    const tab = this.activeTab();
    return this.data.nfes().filter(n => {
      const matchTab =
        tab === 'emitidas'   ? n.status !== 'cancelada' :
        tab === 'canceladas' ? n.status === 'cancelada' : true;
      const matchQ = !q ||
        n.clienteNome.toLowerCase().includes(q) ||
        String(n.numero).includes(q) ||
        n.cfop.includes(q);
      return matchTab && matchQ;
    });
  });

  cancelNfe(id: string) {
    this.data.updateNfeStatus(id, 'cancelada');
    this.toast.warn('NF-e cancelada com sucesso.');
  }

  downloadDanfe(nfe: any) { this.toast.success(`DANFE da NF-e #${nfe.numero} gerado.`); }
  downloadXml(nfe: any)   { this.toast.success(`XML da NF-e #${nfe.numero} baixado.`); }
}
