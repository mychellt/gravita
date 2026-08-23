import { Component, computed } from '@angular/core';
import { NgClass, DatePipe, SlicePipe } from '@angular/common';
import { DataService } from '../../core/services/data.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../shared/pipes/brl.pipe';

@Component({
  selector: 'app-nfse',
  standalone: true,
  imports: [NgClass, DatePipe, SlicePipe, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './nfse.component.scss',
  templateUrl: './nfse.component.html'
})
export class NfseComponent {
  constructor(public data: DataService, private toast: ToastService) {}
  readonly nfses = computed(() => this.data.nfses());
  emitirRps() { this.toast.success('RPS emitido com sucesso!'); }
}
