import { Component, EventEmitter, Input, OnInit, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { nfeFailureDetail } from '../../../core/services/nfe.service';
import { SupplierService } from '../../../core/services/supplier.service';
import { ToastService } from '../../../core/services/toast.service';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { formatCnpj } from './inbound-format';

const STATE_PATTERN = /^[A-Za-z]{2}$/;
const CEP_LENGTH = 8;

/**
 * Cadastra, durante a conferência, o fornecedor da NF-e de entrada (UC-M1-09). O CNPJ é o da nota e não pode ser
 * trocado: é por ele que a nota encontra o fornecedor. O backend exige ao menos um endereço, que o XML não traz;
 * a consulta de CNPJ/CEP adianta o preenchimento, mas nunca é obrigatória.
 */
@Component({
  selector: 'app-register-supplier-modal',
  standalone: true,
  imports: [FormsModule, ModalComponent],
  template: `
    <app-modal title="Cadastrar fornecedor da NF-e" [width]="640" (closed)="closed.emit()">
      <div class="impact-note">
        <i class="ti ti-info-circle"></i>
        <span>
          O CNPJ e o nome vieram da nota. Informe o endereço do fornecedor para cadastrá-lo e seguir com a conferência;
          os itens da nota continuam aqui, sem perder o que você já digitou.
        </span>
      </div>
      <div class="form-row cols-2">
        <div>
          <label class="form-label" for="sup-cnpj">CNPJ</label>
          <input id="sup-cnpj" class="form-input" type="text" readonly [value]="cnpjFormatado" />
        </div>
        <div>
          <label class="form-label" for="sup-nome">Nome / razão social</label>
          <input id="sup-nome" class="form-input" type="text" [class.invalid]="tentouEnviar() && !nome().trim()"
                 [ngModel]="nome()" (ngModelChange)="nome.set($event)" />
          @if (tentouEnviar() && !nome().trim()) { <div class="field-error">Informe o nome do fornecedor.</div> }
        </div>
      </div>
      <div class="form-row cols-3">
        <div>
          <label class="form-label" for="sup-cep">CEP</label>
          <input id="sup-cep" class="form-input" type="text" inputmode="numeric" maxlength="9" [class.invalid]="tentouEnviar() && !cepValido()"
                 [ngModel]="cep()" (ngModelChange)="cep.set($event)" (blur)="buscarCep()" />
          @if (tentouEnviar() && !cepValido()) { <div class="field-error">O CEP tem 8 dígitos.</div> }
        </div>
        <div>
          <label class="form-label" for="sup-rua">Rua</label>
          <input id="sup-rua" class="form-input" type="text" [class.invalid]="tentouEnviar() && !rua().trim()"
                 [ngModel]="rua()" (ngModelChange)="rua.set($event)" />
          @if (tentouEnviar() && !rua().trim()) { <div class="field-error">Informe a rua.</div> }
        </div>
        <div>
          <label class="form-label" for="sup-numero">Número</label>
          <input id="sup-numero" class="form-input" type="text" [ngModel]="numero()" (ngModelChange)="numero.set($event)" />
        </div>
      </div>
      <div class="form-row cols-3">
        <div>
          <label class="form-label" for="sup-bairro">Bairro</label>
          <input id="sup-bairro" class="form-input" type="text" [class.invalid]="tentouEnviar() && !bairro().trim()"
                 [ngModel]="bairro()" (ngModelChange)="bairro.set($event)" />
          @if (tentouEnviar() && !bairro().trim()) { <div class="field-error">Informe o bairro.</div> }
        </div>
        <div>
          <label class="form-label" for="sup-cidade">Cidade</label>
          <input id="sup-cidade" class="form-input" type="text" [class.invalid]="tentouEnviar() && !cidade().trim()"
                 [ngModel]="cidade()" (ngModelChange)="cidade.set($event)" />
          @if (tentouEnviar() && !cidade().trim()) { <div class="field-error">Informe a cidade.</div> }
        </div>
        <div>
          <label class="form-label" for="sup-uf">UF</label>
          <input id="sup-uf" class="form-input" type="text" maxlength="2" [class.invalid]="tentouEnviar() && !ufValida()"
                 [ngModel]="uf()" (ngModelChange)="uf.set($event)" />
          @if (tentouEnviar() && !ufValida()) { <div class="field-error">Informe a UF com 2 letras.</div> }
        </div>
      </div>
      <div class="form-row">
        <div>
          <label class="form-label" for="sup-complemento">Complemento</label>
          <input id="sup-complemento" class="form-input" type="text" [ngModel]="complemento()" (ngModelChange)="complemento.set($event)" />
        </div>
      </div>
      @if (erro()) { <div class="field-error" role="alert">{{ erro() }}</div> }
      <div footer>
        <button class="btn btn-ghost" type="button" (click)="closed.emit()">Cancelar</button>
        <button class="btn btn-primary" type="button" [disabled]="enviando()" (click)="salvar()">
          {{ enviando() ? 'Salvando…' : 'Cadastrar fornecedor' }}
        </button>
      </div>
    </app-modal>
  `
})
export class RegisterSupplierModalComponent implements OnInit {
  private readonly service = inject(SupplierService);
  private readonly toast = inject(ToastService);

  /** CNPJ da nota (só dígitos ou formatado). */
  @Input({ required: true }) cnpj!: string;
  @Input() nomeInicial = '';
  /** Emite depois que o fornecedor foi cadastrado. */
  @Output() registered = new EventEmitter<void>();
  @Output() closed = new EventEmitter<void>();

  readonly nome = signal('');
  readonly cep = signal('');
  readonly rua = signal('');
  readonly numero = signal('');
  readonly complemento = signal('');
  readonly bairro = signal('');
  readonly cidade = signal('');
  readonly uf = signal('');

  readonly tentouEnviar = signal(false);
  readonly enviando = signal(false);
  readonly erro = signal('');

  readonly cepValido = computed(() => this.cep().replace(/\D/g, '').length === CEP_LENGTH);
  readonly ufValida = computed(() => STATE_PATTERN.test(this.uf().trim()));
  private readonly valido = computed(() =>
    !!this.nome().trim() && this.cepValido() && !!this.rua().trim() && !!this.bairro().trim()
    && !!this.cidade().trim() && this.ufValida());

  get cnpjFormatado(): string {
    return formatCnpj(this.cnpj);
  }

  ngOnInit(): void {
    this.nome.set(this.nomeInicial);
    void this.preencherPeloCnpj();
  }

  /** Consulta pública do CNPJ: só completa o que o operador ainda não digitou. */
  private async preencherPeloCnpj(): Promise<void> {
    this.aplicar(await this.service.lookupCnpj(this.cnpj));
  }

  async buscarCep(): Promise<void> {
    if (!this.cepValido()) return;
    this.aplicar(await this.service.lookupCep(this.cep()));
  }

  private aplicar(found: Awaited<ReturnType<SupplierService['lookupCnpj']>>): void {
    if (!found) return;
    const fill = (field: ReturnType<typeof signal<string>>, value?: string | null) => {
      if (value && !field().trim()) field.set(value);
    };
    fill(this.nome, found.name);
    const a = found.address;
    if (!a) return;
    fill(this.cep, a.zipCode);
    fill(this.rua, a.street);
    fill(this.numero, a.number);
    fill(this.complemento, a.complement);
    fill(this.bairro, a.neighborhood);
    fill(this.cidade, a.city);
    fill(this.uf, a.state);
  }

  /** POST /api/suppliers; só fecha depois do sucesso, mantendo o formulário aberto nas falhas. */
  async salvar(): Promise<void> {
    this.tentouEnviar.set(true);
    if (!this.valido() || this.enviando()) return;
    this.enviando.set(true);
    this.erro.set('');
    try {
      await this.service.register({
        document: this.cnpj.replace(/\D/g, ''),
        name: this.nome().trim(),
        addresses: [{
          street: this.rua().trim(),
          number: this.numero().trim() || null,
          complement: this.complemento().trim() || null,
          neighborhood: this.bairro().trim(),
          city: this.cidade().trim(),
          state: this.uf().trim().toUpperCase(),
          zipCode: this.cep().replace(/\D/g, ''),
        }],
      });
      this.toast.success('Fornecedor cadastrado — a conferência continua nesta nota');
      this.registered.emit();
    } catch (error) {
      this.erro.set((await nfeFailureDetail(error)) || 'Não foi possível cadastrar o fornecedor.');
      this.enviando.set(false);
    }
  }
}
