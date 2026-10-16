import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { NfeService, nfeFailureDetail } from '../../../core/services/nfe.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';

@Component({
  selector: 'app-import-xml-modal',
  standalone: true,
  imports: [ModalComponent],
  template: `
    <app-modal title="Importar XML de NF-e de entrada" (closed)="closed.emit()">
      <div class="impact-note">
        <i class="ti ti-info-circle"></i>
        <span>Envie o XML da NF-e já obtido do fornecedor. A nota fica aguardando conferência.</span>
      </div>
      <label class="form-label" for="imp-xml">Arquivo XML</label>
      <input id="imp-xml" class="form-input" type="file" accept=".xml,text/xml,application/xml"
             [class.invalid]="tentouEnviar() && !arquivo()"
             (change)="escolher($any($event.target).files)" />
      @if (arquivo(); as file) { <div class="field-hint">{{ file.name }}</div> }
      @if (tentouEnviar() && !arquivo()) { <div class="field-error">Escolha o arquivo XML.</div> }
      @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="enviar()">
          {{ enviando() ? 'Importando…' : 'Importar' }}
        </button>
      </div>
    </app-modal>
  `
})
export class ImportXmlModalComponent {
  private readonly nfe = inject(NfeService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  @Output() closed = new EventEmitter<void>();

  readonly arquivo = signal<File | null>(null);
  readonly tentouEnviar = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal('');

  escolher(files: FileList | null): void {
    this.arquivo.set(files?.item(0) ?? null);
    this.erro.set('');
  }

  /** POST /api/nfe/inbound/import-xml; só fecha depois da resposta de sucesso, mantendo o modal aberto nas falhas. */
  async enviar(): Promise<void> {
    this.tentouEnviar.set(true);
    const arquivo = this.arquivo();
    const companyId = this.auth.currentUser()?.companyId;
    if (!arquivo || this.enviando()) return;
    if (!companyId) return this.erro.set('Usuário sem empresa vinculada.');
    this.enviando.set(true);
    this.erro.set('');
    try {
      const nota = await this.nfe.importInboundXml(companyId, arquivo);
      if (nota.supplierResolved) {
        this.toast.success(`NF-e de ${nota.supplierName} importada — aguardando conferência`);
      } else {
        this.toast.warn(`NF-e de ${nota.supplierName} importada, mas o fornecedor ainda não está cadastrado — cadastre-o antes de conferir.`);
      }
      this.closed.emit();
    } catch (error) {
      this.erro.set((await nfeFailureDetail(error)) || 'Não foi possível importar o XML.');
      this.enviando.set(false);
    }
  }
}
