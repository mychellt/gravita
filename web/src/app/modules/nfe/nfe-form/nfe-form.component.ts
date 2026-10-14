import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { IE_INDICATOR_LABELS, formatAddress } from '../../../core/customer-display';
import { AuthService } from '../../../core/services/auth.service';
import { CfopRegistryService } from '../../../core/services/cfop-registry.service';
import { Customer, CustomerAddress, CustomerService } from '../../../core/services/customer.service';
import {
  IssueNfeRequest, IssueNfeResponse, NaturezaOperacao, NfeService, TransportModality, nfeFailureDetail,
} from '../../../core/services/nfe.service';
import { PaymentMethodService } from '../../../core/services/payment-method.service';
import { ProductSearchResult, ProductSearchService } from '../../../core/services/product-search.service';
import { TransportModalityCode, TransportModalityService } from '../../../core/services/transport-modality.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { BrlPipe } from '../../../shared/pipes/brl.pipe';

export interface FormItem {
  productId: string;
  descricao: string;
  ncm: string | null;
  quantidade: number;
  valorUnitario: number;
  /** Percentual, 0-100. */
  desconto: number;
  /** Nasce com o CFOP do registro para a natureza escolhida; editável por linha. */
  cfop: string;
}

/** Código da modalidade escolhida no cadastro (''= nenhuma). "NONE" = sem transporte: nada de transporte é enviado. */
export type ModalidadeChoice = '' | TransportModalityCode;

export type NfeStepNumber = 1 | 2 | 3 | 4 | 5;

const GENERIC_FAILURE = 'Não foi possível emitir a NF-e. Tente novamente.';
const MAX_CUSTOMER_MATCHES = 8;
const ACCESS_KEY_LENGTH = 44;
const digitsOnly = (value: string) => value.replace(/\D/g, '');

const round2 = (value: number) => Math.round(value * 100) / 100;
const subtotal = (item: FormItem) => item.quantidade * item.valorUnitario;
export const lineTotal = (item: FormItem) => round2(subtotal(item) * (1 - item.desconto / 100));

@Component({
  selector: 'app-nfe-form',
  standalone: true,
  imports: [FormsModule, RouterLink, BadgeComponent, PageHeaderComponent, BrlPipe],
  styleUrl: './nfe-form.component.scss',
  templateUrl: './nfe-form.component.html'
})
export class NfeFormComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly customerService = inject(CustomerService);
  private readonly productSearch = inject(ProductSearchService);
  private readonly nfeService = inject(NfeService);
  private readonly cfopRegistry = inject(CfopRegistryService);
  private readonly paymentMethodService = inject(PaymentMethodService);
  private readonly transportModalityService = inject(TransportModalityService);

  readonly steps = [
    { n: 1, label: 'Natureza' },
    { n: 2, label: 'Destinatário' },
    { n: 3, label: 'Itens' },
    { n: 4, label: 'Transporte' },
    { n: 5, label: 'Revisão' },
  ] as const;
  readonly ieLabels = IE_INDICATOR_LABELS;
  readonly lineTotal = lineTotal;

  /** Só destaca o passo clicado; o "feito" de cada passo vem do estado do formulário (`stepDone`). */
  currentStep = signal<NfeStepNumber>(1);

  // Natureza (as opções e o CFOP de cada uma vêm do registro de CFOP do backend)
  readonly naturezaOptions = this.cfopRegistry.entries;
  readonly registryStatus = this.cfopRegistry.status;
  naturezaSel = signal<NaturezaOperacao | ''>('');
  referencedAccessKey = signal('');

  readonly naturezaEntry = computed(() => this.naturezaOptions().find(e => e.naturezaOperacao === this.naturezaSel()) ?? null);
  /** CFOP que as linhas novas recebem; vazio enquanto nenhuma natureza foi escolhida. */
  readonly naturezaCfop = computed(() => this.naturezaEntry()?.cfop ?? '');
  readonly requiresReferencedAccessKey = computed(() => this.naturezaEntry()?.requiresReferencedAccessKey ?? false);
  readonly referencedAccessKeyInvalid = computed(() =>
    this.requiresReferencedAccessKey() && digitsOnly(this.referencedAccessKey()).length !== ACCESS_KEY_LENGTH);

  // Destinatário
  readonly customerStatus = this.customerService.status;
  customerQuery = signal('');
  customer = signal<Customer | null>(null);

  readonly customerMatches = computed(() => {
    const q = this.customerQuery().trim().toLowerCase();
    if (!q || this.customer()) return [];
    const digits = q.replace(/\D/g, '');
    return this.customerService.customers()
      .filter(c => c.name.toLowerCase().includes(q) || c.document.toLowerCase().includes(q)
        || (digits !== '' && c.document.replace(/\D/g, '').includes(digits)))
      .slice(0, MAX_CUSTOMER_MATCHES);
  });

  /** Endereço de cobrança padrão; senão o padrão qualquer; senão o primeiro. */
  readonly recipientAddress = computed<CustomerAddress | null>(() => {
    const addresses = this.customer()?.addresses ?? [];
    return addresses.find(a => a.isDefault && a.type === 'BILLING') ?? addresses.find(a => a.isDefault) ?? addresses[0] ?? null;
  });
  readonly recipientState = computed(() => this.recipientAddress()?.state?.trim() || null);

  // Itens
  items = signal<FormItem[]>([]);
  productQuery = signal('');
  productResults = signal<ProductSearchResult[]>([]);
  productSearchStatus = signal<'idle' | 'loading' | 'ready' | 'error'>('idle');
  private productSearchSeq = 0;

  // Transporte (as opções vêm de GET /api/auxiliary/transport-modalities; o valor escolhido é o código da linha)
  readonly modalidadeOptions = this.transportModalityService.modalities;
  readonly modalidadesStatus = this.transportModalityService.status;
  modalidade = signal<ModalidadeChoice>('');
  freight = signal<number | null>(null);
  grossWeight = signal<number | null>(null);

  // Pagamento (as opções vêm de GET /api/auxiliary/payment-methods; escolher uma é opcional)
  readonly paymentMethods = this.paymentMethodService.paymentMethods;
  readonly paymentMethodsStatus = this.paymentMethodService.status;
  paymentMethodId = signal('');
  infoAdicionais = signal('');

  // Emissão
  submitting = signal(false);
  errorMessage = signal<string | null>(null);
  issued = signal<IssueNfeResponse | null>(null);

  readonly baseCalculo = computed(() => round2(this.items().reduce((sum, i) => sum + lineTotal(i), 0)));
  readonly icms        = computed(() => this.baseCalculo() * 0.18);
  readonly pis         = computed(() => this.baseCalculo() * 0.0065);
  readonly cofins      = computed(() => this.baseCalculo() * 0.03);
  readonly totalNota   = computed(() => this.baseCalculo());

  /** Estado de cada passo, derivado do formulário e nunca da navegação. */
  readonly stepDone = computed<Record<NfeStepNumber, boolean>>(() => {
    const done = {
      1: this.naturezaSel() !== '' && !this.referencedAccessKeyInvalid(),
      2: this.customer() !== null,
      3: this.items().length > 0,
      4: this.modalidade() !== '',
    };
    return { ...done, 5: Object.values(done).every(Boolean) };
  });

  readonly itemsValid = computed(() => this.items().every(i => i.quantidade > 0 && i.valorUnitario >= 0));

  /** Cliente sem UF no endereço não tem como virar destinatário da NF-e. */
  readonly recipientIncomplete = computed(() => this.customer() !== null && this.recipientState() === null);

  readonly canEmit = computed(() =>
    this.stepDone()[5] && this.itemsValid() && !this.recipientIncomplete() && !this.submitting() && this.issued() === null);

  ngOnInit() {
    void this.customerService.load();
    void this.cfopRegistry.load();
    void this.paymentMethodService.load();
    void this.transportModalityService.load();
  }

  reloadRegistry() {
    void this.cfopRegistry.load();
  }

  reloadPaymentMethods() {
    void this.paymentMethodService.load();
  }

  reloadTransportModalities() {
    void this.transportModalityService.load();
  }

  reloadCustomers() {
    void this.customerService.load();
  }

  selectCustomer(customer: Customer) {
    this.customer.set(customer);
    this.customerQuery.set('');
  }

  clearCustomer() {
    this.customer.set(null);
  }

  async searchProducts() {
    const term = this.productQuery().trim();
    if (!term) return;
    const seq = ++this.productSearchSeq;
    this.productSearchStatus.set('loading');
    try {
      const results = await this.productSearch.search(term, this.customer()?.id);
      if (seq !== this.productSearchSeq) return;
      this.productResults.set(results);
      this.productSearchStatus.set('ready');
    } catch {
      if (seq !== this.productSearchSeq) return;
      this.productResults.set([]);
      this.productSearchStatus.set('error');
    }
  }

  /** Um produto indisponível para venda nunca entra na nota. */
  addProduct(product: ProductSearchResult) {
    if (!product.availableForSale) return;
    this.items.update(list => {
      const existing = list.find(i => i.productId === product.productId);
      if (existing) {
        return list.map(i => (i === existing ? { ...i, quantidade: i.quantidade + 1 } : i));
      }
      return [...list, {
        productId: product.productId, descricao: product.description, ncm: product.ncm,
        quantidade: 1, valorUnitario: product.unitPrice, desconto: 0, cfop: this.naturezaCfop(),
      }];
    });
  }

  updateItem(index: number, patch: Partial<Pick<FormItem, 'quantidade' | 'valorUnitario' | 'desconto' | 'cfop'>>) {
    const clean = { ...patch };
    if (clean.desconto !== undefined) clean.desconto = Math.min(100, Math.max(0, clean.desconto));
    this.items.update(list => list.map((item, idx) => (idx === index ? { ...item, ...clean } : item)));
  }

  removeItem(i: number) {
    this.items.update(list => list.filter((_, idx) => idx !== i));
  }

  async emit() {
    if (!this.canEmit()) return;
    const request = this.buildRequest();
    if (!request) {
      this.errorMessage.set('Não foi possível identificar a empresa emitente. Entre novamente e tente de novo.');
      return;
    }
    this.submitting.set(true);
    this.errorMessage.set(null);
    try {
      this.issued.set(await this.nfeService.issue(request));
    } catch (error) {
      this.errorMessage.set((await nfeFailureDetail(error)) || GENERIC_FAILURE);
    } finally {
      this.submitting.set(false);
    }
  }

  cancel() { this.router.navigate(['/nfe']); }

  goToList() { this.router.navigate(['/nfe']); }

  private buildRequest(): IssueNfeRequest | null {
    const issuerCompanyId = this.auth.currentUser()?.companyId;
    const customer = this.customer();
    const state = this.recipientState();
    const natureza = this.naturezaSel();
    if (!issuerCompanyId || !customer || !state || !natureza) return null;

    const modalidade = this.modalidade();
    const additionalInfo = this.infoAdicionais().trim();
    const paymentMethodId = this.paymentMethodId();
    return {
      issuerCompanyId,
      naturezaOperacao: natureza,
      recipient: {
        personId: customer.id,
        document: customer.document,
        personType: customer.type,
        name: customer.name,
        // O cadastro só guarda o indicador de IE, não o número: só "isento" tem o que enviar.
        stateRegistration: customer.ieIndicator === 'EXEMPT' ? 'ISENTO' : undefined,
        state,
      },
      items: this.items().map(i => ({
        productId: i.productId,
        description: i.descricao,
        quantity: i.quantidade,
        unitPrice: i.valorUnitario,
        // A tela edita percentual; o backend recebe o valor em reais.
        discount: round2(subtotal(i) * i.desconto / 100),
      })),
      ...(modalidade === 'CIF' || modalidade === 'FOB' ? this.transportFields(modalidade) : {}),
      ...(this.requiresReferencedAccessKey() ? { referencedAccessKey: digitsOnly(this.referencedAccessKey()) } : {}),
      ...(additionalInfo ? { additionalInfo } : {}),
      ...(paymentMethodId ? { paymentMethodId } : {}),
    };
  }

  private transportFields(modality: TransportModality): Pick<IssueNfeRequest, 'freight' | 'transport'> {
    const freight = this.freight();
    const grossWeight = this.grossWeight();
    return {
      ...(freight ? { freight } : {}),
      transport: { modality, ...(grossWeight ? { grossWeight } : {}) },
    };
  }

  readonly addressText = formatAddress;
}
