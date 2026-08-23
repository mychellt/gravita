import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./modules/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'pdv',
        loadComponent: () => import('./modules/pdv/pdv.component').then(m => m.PdvComponent)
      },
      {
        path: 'nfe',
        loadComponent: () => import('./modules/nfe/nfe.component').then(m => m.NfeComponent)
      },
      {
        path: 'nfe/nova',
        loadComponent: () => import('./modules/nfe/nfe-form/nfe-form.component').then(m => m.NfeFormComponent)
      },
      {
        path: 'nfse',
        loadComponent: () => import('./modules/nfse/nfse.component').then(m => m.NfseComponent)
      },
      {
        path: 'estoque',
        loadComponent: () => import('./modules/estoque/estoque.component').then(m => m.EstoqueComponent)
      },
      {
        path: 'compras',
        loadComponent: () => import('./modules/compras/compras.component').then(m => m.ComprasComponent)
      },
      {
        path: 'crm',
        loadComponent: () => import('./modules/crm/crm.component').then(m => m.CrmComponent)
      },
      {
        path: 'financeiro',
        loadComponent: () => import('./modules/financeiro/financeiro.component').then(m => m.FinanceiroComponent)
      },
      {
        path: 'relatorios',
        loadComponent: () => import('./modules/relatorios/relatorios.component').then(m => m.RelatoriosComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./modules/settings/settings.component').then(m => m.SettingsComponent)
      },
    ]
  },
  { path: '**', redirectTo: '' }
];
