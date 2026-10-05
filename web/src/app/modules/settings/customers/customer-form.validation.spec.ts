import { AddressDraft, CustomerDraft, emptyAddress, emptyDraft, isValidCpf, parseMoney, toNewCustomer, validateCustomerDraft } from './customer-form.validation';

const address = (over: Partial<AddressDraft> = {}): AddressDraft =>
  ({ ...emptyAddress('BILLING', true), street: 'Rua A', number: '10', neighborhood: 'Centro', city: 'São Paulo', state: 'SP', zipCode: '01310-100', ...over });

const company = (over: Partial<CustomerDraft> = {}): CustomerDraft =>
  ({ ...emptyDraft(), name: 'Acme Ltda', document: '11.222.333/0001-81', ieIndicator: 'TAXPAYER', finalConsumer: 'no', addresses: [address()], ...over });

const person = (over: Partial<CustomerDraft> = {}): CustomerDraft =>
  ({ ...emptyDraft(), type: 'INDIVIDUAL', name: 'Ana Souza', document: '529.982.247-25', addresses: [address()], ...over });

describe('customer registration validation', () => {
  it('accepts a complete company and a complete person', () => {
    expect(validateCustomerDraft(company())).toEqual({});
    expect(validateCustomerDraft(person())).toEqual({});
  });

  it('requires a name and a valid document for the chosen type', () => {
    expect(validateCustomerDraft(company({ name: ' ', document: '' }))).toEqual(
      jasmine.objectContaining({ name: 'Informe o nome.', document: 'Informe o CNPJ.' }));
    expect(validateCustomerDraft(company({ document: '11.222.333/0001-80' }))['document']).toBe('CNPJ inválido.');
    expect(validateCustomerDraft(person({ document: '111.111.111-11' }))['document']).toBe('CPF inválido.');
    expect(isValidCpf('529.982.247-25')).toBeTrue();
  });

  it('requires the IE indicator and the final-consumer flag only for a company', () => {
    expect(validateCustomerDraft(company({ ieIndicator: '', finalConsumer: '' }))).toEqual(
      jasmine.objectContaining({ ieIndicator: jasmine.any(String), finalConsumer: jasmine.any(String) }));
    expect(validateCustomerDraft(person())).toEqual({});
  });

  it('needs at least one filled-in address (an untouched row does not count)', () => {
    expect(validateCustomerDraft(company({ addresses: [emptyAddress('BILLING', true)] }))['addresses'])
      .toBe('Informe ao menos um endereço completo.');
    expect(validateCustomerDraft(company({ addresses: [] }))['addresses']).toBeDefined();
  });

  it('reports the missing fields of a half-filled address', () => {
    const errors = validateCustomerDraft(company({ addresses: [address({ city: '', zipCode: '123', state: 'Sao' })] }));

    expect(errors['addresses.0.city']).toBe('Obrigatório.');
    expect(errors['addresses.0.zipCode']).toBe('CEP inválido.');
    expect(errors['addresses.0.state']).toBe('Use a sigla (UF).');
    expect(errors['addresses']).toBeDefined();
  });

  it('wants exactly one default address per address type, as the backend does', () => {
    const none = company({ addresses: [address({ isDefault: false })] });
    const two = company({ addresses: [address(), address({ street: 'Rua B' })] });
    const oneEach = company({ addresses: [address(), address({ type: 'DELIVERY', street: 'Rua B' })] });

    expect(validateCustomerDraft(none)['defaults.BILLING']).toBeDefined();
    expect(validateCustomerDraft(two)['defaults.BILLING']).toBeDefined();
    expect(validateCustomerDraft(oneEach)).toEqual({});
  });

  it('checks the optional e-mail, credit limit and contacts', () => {
    const errors = validateCustomerDraft(company({
      email: 'nope', creditLimit: '-5',
      contacts: [{ type: 'PHONE', value: '123' }, { type: 'EMAIL', value: '' }, { type: 'EMAIL', value: 'a@b.co' }],
    }));

    expect(errors['email']).toBe('E-mail inválido.');
    expect(errors['creditLimit']).toBeDefined();
    expect(errors['contacts.0']).toBeDefined();
    expect(errors['contacts.1']).toBeUndefined();
    expect(errors['contacts.2']).toBeUndefined();
  });

  it('parses money typed the Brazilian way', () => {
    expect(parseMoney('1.500,50')).toBe(1500.5);
    expect(parseMoney('1500.5')).toBe(1500.5);
    expect(parseMoney('')).toBeNull();
    expect(parseMoney('abc')).toBeNull();
  });

  it('builds the request without the untouched rows and with digits-only document and zip', () => {
    const body = toNewCustomer(company({
      email: ' vendas@acme.com ', creditLimit: '2.000,00',
      addresses: [address(), emptyAddress('DELIVERY', true)],
      contacts: [{ type: 'PHONE', value: '' }, { type: 'WHATSAPP', value: '(11) 91234-5678' }],
    }));

    expect(body).toEqual({
      type: 'COMPANY', document: '11222333000181', name: 'Acme Ltda', email: 'vendas@acme.com',
      ieIndicator: 'TAXPAYER', finalConsumer: false, creditLimit: 2000,
      addresses: [{ type: 'BILLING', street: 'Rua A', number: '10', complement: null, neighborhood: 'Centro',
        city: 'São Paulo', state: 'SP', zipCode: '01310100', isDefault: true }],
      contacts: [{ type: 'WHATSAPP', value: '(11) 91234-5678' }],
    });
  });

  it('leaves the company-only fields out of a person request', () => {
    const body = toNewCustomer(person());
    expect(body.ieIndicator).toBeUndefined();
    expect(body.finalConsumer).toBeUndefined();
    expect(body.document).toBe('52998224725');
  });
});
