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
  '/admin/configuracoes': 'Configurações gerais da plataforma',
  '/admin/clientes': 'Clientes Gravita',
};

function titleFor(url: string): string {
  const path = url.split(/[?#]/)[0];
  if (ROUTE_TITLES[path]) return ROUTE_TITLES[path];
  const prefix = Object.keys(ROUTE_TITLES)
    .filter(k => path.startsWith(k + '/'))
    .sort((a, b) => b.length - a.length)[0];
  return prefix ? ROUTE_TITLES[prefix] : 'Gravita';
}

@Injectable({ providedIn: 'root' })
export class TitleService {
  title = signal('Dashboard');

  constructor(router: Router) {
    router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: any) => {
      this.title.set(titleFor(e.urlAfterRedirects));
    });
  }
}
