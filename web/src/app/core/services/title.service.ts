import { Injectable, signal } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

const ROUTE_TITLES: Record<string, string> = {
  '/dashboard':  'Dashboard',
  '/pdv':        'PDV / NFC-e',
  '/nfe':        'NF-e Modelo 55',
  '/nfe/nova':   'Nova NF-e',
  '/nfse':       'NFS-e',
  '/estoque':    'Estoque',
  '/compras':    'Compras',
  '/crm':        'Vendas & CRM',
  '/financeiro': 'Financeiro',
  '/relatorios': 'Relatórios & BI',
  '/settings':   'Configurações',
};

@Injectable({ providedIn: 'root' })
export class TitleService {
  title = signal('Dashboard');

  constructor(router: Router) {
    router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: any) => {
      this.title.set(ROUTE_TITLES[e.urlAfterRedirects] ?? 'Gravita');
    });
  }
}
