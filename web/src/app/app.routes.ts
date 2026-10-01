import { Routes } from '@angular/router';
import { unsavedChangesGuard } from './core/guards/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: 'admin',
    loadComponent: () => import('./layout/admin-shell/admin-shell.component').then(m => m.AdminShellComponent),
    children: [
      { path: '', redirectTo: 'configuracoes/planos', pathMatch: 'full' },
      { path: 'configuracoes', redirectTo: 'configuracoes/planos', pathMatch: 'full' },
      {
        path: 'configuracoes/:secao',
        loadComponent: () => import('./modules/admin/platform-settings/platform-settings.component').then(m => m.PlatformSettingsComponent),
        canDeactivate: [unsavedChangesGuard]
      },
      {
        path: 'clientes',
        loadComponent: () => import('./modules/admin/customers/platform-customers.component').then(m => m.PlatformCustomersComponent)
      },
      {
        path: 'clientes/:id',
        loadComponent: () => import('./modules/admin/customers/platform-customer-detail.component').then(m => m.PlatformCustomerDetailComponent)
      },
      {
        path: 'clientes/:id/pagamentos',
        loadComponent: () => import('./modules/admin/customers/platform-customer-payments.component').then(m => m.PlatformCustomerPaymentsComponent)
      },
    ]
  },
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
      {
        path: 'settings/clientes',
        loadComponent: () => import('./modules/settings/clientes/clientes-list.component').then(m => m.ClientesListComponent)
      },
      {
        path: 'settings/clientes/:id',
        loadComponent: () => import('./modules/settings/clientes/cliente-detail.component').then(m => m.ClienteDetailComponent)
      },
      {
        path: 'settings/clientes/:id/pagamentos',
        loadComponent: () => import('./modules/settings/clientes/cliente-pagamentos.component').then(m => m.ClientePagamentosComponent)
      },
    ]
  },
  { path: '**', redirectTo: '' }
];
