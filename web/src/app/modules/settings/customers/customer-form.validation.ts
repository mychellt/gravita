import { AddressKind, ContactKind, IeIndicator, NewCustomer, PersonKind } from '../../../core/services/customer.service';
import { isValidCnpj } from '../../admin/platform-settings/platform-settings.validation';

export interface AddressDraft {
  type: AddressKind;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  isDefault: boolean;
}

export interface ContactDraft {
  type: ContactKind;
  value: string;
}

export interface CustomerDraft {
  type: PersonKind;
  document: string;
  name: string;
  email: string;
  ieIndicator: IeIndicator | '';
  finalConsumer: 'yes' | 'no' | '';
  creditLimit: string;
  addresses: AddressDraft[];
  contacts: ContactDraft[];
}

/** Chaves: `name`, `document`, `email`, `ieIndicator`, `finalConsumer`, `creditLimit`, `addresses`, `addresses.<i>.<campo>`, `defaults.<tipo>`, `contacts.<i>`. */
export type CustomerFormErrors = Record<string, string>;

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const digitsOf = (value: string) => value.replace(/\D/g, '');
const isBlank = (value: string) => value.trim() === '';

export const emptyAddress = (type: AddressKind = 'BILLING', isDefault = false): AddressDraft =>
  ({ type, street: '', number: '', complement: '', neighborhood: '', city: '', state: '', zipCode: '', isDefault });

export const emptyDraft = (): CustomerDraft => ({
  type: 'COMPANY', document: '', name: '', email: '', ieIndicator: '', finalConsumer: '', creditLimit: '',
  addresses: [emptyAddress('BILLING', true)], contacts: [],
});

export function isValidCpf(raw: string): boolean {
  const n = digitsOf(raw);
  if (n.length !== 11 || /^(\d)\1+$/.test(n)) return false;
  const digit = (length: number) => {
    const sum = n.slice(0, length).split('').reduce((s, d, i) => s + Number(d) * (length + 1 - i), 0);
    const rest = (sum * 10) % 11;
    return rest === 10 ? 0 : rest;
  };
  return Number(n[9]) === digit(9) && Number(n[10]) === digit(10);
}

/** Aceita "1500", "1500.5" e "1.500,50"; devolve `null` quando não é um número. */
export function parseMoney(text: string): number | null {
  const t = text.trim();
  if (t === '') return null;
  const normalized = t.includes(',') ? t.replace(/\./g, '').replace(',', '.') : t;
  const value = Number(normalized);
  return Number.isFinite(value) ? value : null;
}

/** Uma linha de endereço "iniciada" é qualquer uma em que o usuário digitou algo; linhas intocadas são ignoradas. */
const isStarted = (a: AddressDraft) =>
  [a.street, a.number, a.complement, a.neighborhood, a.city, a.state, a.zipCode].some(v => !isBlank(v));

export const startedAddresses = (draft: CustomerDraft) => draft.addresses.filter(isStarted);

/**
 * As mesmas regras do backend (`CustomerDomain.validateForRegistration`), aplicadas antes do envio:
 * nome e documento válidos; PJ com indicador de IE e consumidor final; ao menos um endereço completo;
 * e exatamente um endereço padrão por tipo de endereço presente.
 */
export function validateCustomerDraft(draft: CustomerDraft): CustomerFormErrors {
  const e: CustomerFormErrors = {};

  if (isBlank(draft.name)) e['name'] = 'Informe o nome.';

  if (isBlank(draft.document)) e['document'] = draft.type === 'COMPANY' ? 'Informe o CNPJ.' : 'Informe o CPF.';
  else if (draft.type === 'COMPANY' ? !isValidCnpj(draft.document) : !isValidCpf(draft.document)) {
    e['document'] = draft.type === 'COMPANY' ? 'CNPJ inválido.' : 'CPF inválido.';
  }

  if (!isBlank(draft.email) && !EMAIL.test(draft.email.trim())) e['email'] = 'E-mail inválido.';

  if (draft.type === 'COMPANY') {
    if (draft.ieIndicator === '') e['ieIndicator'] = 'Informe o indicador de IE.';
    if (draft.finalConsumer === '') e['finalConsumer'] = 'Informe se é consumidor final.';
  }

  const credit = parseMoney(draft.creditLimit);
  if (!isBlank(draft.creditLimit) && (credit === null || credit < 0)) e['creditLimit'] = 'Informe um valor maior ou igual a zero.';

  validateAddresses(draft, e);

  draft.contacts.forEach((c, i) => {
    if (isBlank(c.value)) return;
    const ok = c.type === 'EMAIL' ? EMAIL.test(c.value.trim()) : digitsOf(c.value).length >= 10;
    if (!ok) e[`contacts.${i}`] = c.type === 'EMAIL' ? 'E-mail inválido.' : 'Informe o telefone com DDD.';
  });

  return e;
}

function validateAddresses(draft: CustomerDraft, e: CustomerFormErrors) {
  const required: (keyof AddressDraft)[] = ['street', 'neighborhood', 'city', 'state', 'zipCode'];
  let complete = 0;
  draft.addresses.forEach((a, i) => {
    if (!isStarted(a)) return;
    let rowComplete = true;
    for (const field of required) {
      if (isBlank(String(a[field]))) { e[`addresses.${i}.${field}`] = 'Obrigatório.'; rowComplete = false; }
    }
    if (!isBlank(a.state) && !/^[A-Za-z]{2}$/.test(a.state.trim())) { e[`addresses.${i}.state`] = 'Use a sigla (UF).'; rowComplete = false; }
    if (!isBlank(a.zipCode) && digitsOf(a.zipCode).length !== 8) { e[`addresses.${i}.zipCode`] = 'CEP inválido.'; rowComplete = false; }
    if (rowComplete) complete++;
  });

  if (complete === 0) e['addresses'] = 'Informe ao menos um endereço completo.';

  for (const type of ['BILLING', 'DELIVERY'] as AddressKind[]) {
    const ofType = startedAddresses(draft).filter(a => a.type === type);
    if (ofType.length > 0 && ofType.filter(a => a.isDefault).length !== 1) {
      e[`defaults.${type}`] = 'Marque exatamente um endereço padrão para este tipo.';
    }
  }
}

/** Monta o corpo do POST ignorando linhas vazias; só deve ser chamado com um rascunho sem erros. */
export function toNewCustomer(draft: CustomerDraft): NewCustomer {
  const credit = parseMoney(draft.creditLimit);
  const isCompany = draft.type === 'COMPANY';
  return {
    type: draft.type,
    document: digitsOf(draft.document),
    name: draft.name.trim(),
    ...(isBlank(draft.email) ? {} : { email: draft.email.trim() }),
    ...(isCompany && draft.ieIndicator !== '' ? { ieIndicator: draft.ieIndicator } : {}),
    ...(isCompany && draft.finalConsumer !== '' ? { finalConsumer: draft.finalConsumer === 'yes' } : {}),
    ...(credit !== null ? { creditLimit: credit } : {}),
    addresses: startedAddresses(draft).map(a => ({
      type: a.type, street: a.street.trim(), number: isBlank(a.number) ? null : a.number.trim(),
      complement: isBlank(a.complement) ? null : a.complement.trim(), neighborhood: a.neighborhood.trim(),
      city: a.city.trim(), state: a.state.trim().toUpperCase(), zipCode: digitsOf(a.zipCode), isDefault: a.isDefault,
    })),
    contacts: draft.contacts.filter(c => !isBlank(c.value)).map(c => ({ type: c.type, value: c.value.trim() })),
  };
}
