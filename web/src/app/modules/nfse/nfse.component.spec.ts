import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NfseListItem } from '../../core/services/nfse.service';
import { NfseComponent } from './nfse.component';

const item = (over: Partial<NfseListItem> = {}): NfseListItem => ({
  id: 'n1', status: 'AUTHORIZED', series: '1', number: 10, rpsSeries: 'RPS', rpsNumber: 3, tomadorName: 'Tomador Alfa',
  tomadorDocument: '52998224725', municipalityIbgeCode: '3550308', municipalityName: 'São Paulo', municipalityState: 'SP',
  serviceCode: '1.05', discrimination: 'Consultoria em tecnologia da informação mensal', serviceAmount: 1000, issRate: 5,
  issAmount: 50, withheldAmount: 10, issuedAt: new Date().toISOString(), ...over,
});

describe('NfseComponent', () => {
  let http: HttpTestingController;

  async function create(response: NfseListItem[] | 'fail') {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(NfseComponent);
    fixture.detectChanges();
    const request = http.expectOne('/api/nfse');
    response === 'fail' ? request.flush(null, { status: 500, statusText: 'Server Error' }) : request.flush(response);
    await new Promise(r => setTimeout(r));
    fixture.detectChanges();
    return fixture;
  }
  afterEach(() => http.verify());

  it('lists the NFS-e from the backend with municipality and amounts', async () => {
    const fixture = await create([item(), item({ id: 'n2', status: 'RPS', number: null, municipalityName: null, municipalityState: null })]);
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain('Tomador Alfa');
    expect(text).toContain('São Paulo/SP');
    expect(text).toContain('3550308');
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(2);
  });

  it('sums the month KPIs without the cancelled documents', async () => {
    const fixture = await create([item(), item({ id: 'n2', status: 'CANCELLED', serviceAmount: 500, issAmount: 25 })]);
    const kpis = fixture.componentInstance.kpis();

    expect(kpis.count).toBe(1);
    expect(kpis.serviceTotal).toBe(1000);
    expect(kpis.issTotal).toBe(50);
  });

  it('shows an empty state when there are none', async () => {
    const fixture = await create([]);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Nenhuma NFS-e encontrada');
  });

  it('offers a retry when the list cannot be loaded', async () => {
    const fixture = await create('fail');

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Não foi possível carregar as NFS-e.');
    (fixture.nativeElement.querySelector('.nfse-state-error button') as HTMLButtonElement).click();
    http.expectOne('/api/nfse').flush([item()]);
    await new Promise(r => setTimeout(r));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Tomador Alfa');
  });
});
