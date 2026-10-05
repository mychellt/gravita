import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { unsavedChangesGuard } from './core/guards/unsaved-changes.guard';

export const routes: Routes = [
  {
    path: 'admin',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    loadComponent: () => import('./layout/admin-shell/admin-shell.component').then(m => m.AdminShellComponent),
    children: [
      { path: '', redirectTo: 'settings/planos', pathMatch: 'full' },
      { path: 'settings', redirectTo: 'settings/planos', pathMatch: 'full' },
      {
        path: 'settings/:section',
        loadComponent: () => import('./modules/admin/platform-settings/platform-settings.component').then(m => m.PlatformSettingsComponent),
        canDeactivate: [unsavedChangesGuard]
      },
      {
        path: 'customers',
        loadComponent: () => import('./modules/admin/customers/platform-customers.component').then(m => m.PlatformCustomersComponent)
      },
      {
        path: 'customers/:id',
        loadComponent: () => import('./modules/admin/customers/platform-customer-detail.component').then(m => m.PlatformCustomerDetailComponent)
      },
      {
        path: 'customers/:id/payments',
        loadComponent: () => import('./modules/admin/customers/platform-customer-payments.component').then(m => m.PlatformCustomerPaymentsComponent)
      },
    ]
  },
  // Telas públicas de autenticação: fora do shell do app. O login redireciona quem já tem sessão.
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./modules/auth/login/login.component').then(m => m.LoginComponent)
  },
  // Telas públicas de recuperação de senha: fora do shell do app, sem sessão.
  {
    path: 'forgot-password',
    loadComponent: () => import('./modules/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./modules/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    canActivateChild: [authGuard],
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
        path: 'inventory',
        loadComponent: () => import('./modules/inventory/inventory.component').then(m => m.InventoryComponent)
      },
      {
        path: 'purchasing',
        loadComponent: () => import('./modules/purchasing/purchasing.component').then(m => m.PurchasingComponent)
      },
      {
        path: 'crm',
        loadComponent: () => import('./modules/crm/crm.component').then(m => m.CrmComponent)
      },
      {
        path: 'finance',
        loadComponent: () => import('./modules/finance/finance.component').then(m => m.FinanceComponent)
      },
      {
        path: 'reports',
        loadComponent: () => import('./modules/reports/reports.component').then(m => m.ReportsComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./modules/settings/settings.component').then(m => m.SettingsComponent),
        // Clientes vive dentro do layout de Configurações (menu lateral + painel).
        children: [
          {
            path: 'customers',
            loadComponent: () => import('./modules/settings/customers/customers-list.component').then(m => m.CustomersListComponent)
          },
          {
            path: 'customers/new',
            loadComponent: () => import('./modules/settings/customers/customer-form.component').then(m => m.CustomerFormComponent)
          },
          {
            path: 'customers/:id',
            loadComponent: () => import('./modules/settings/customers/customer-detail.component').then(m => m.CustomerDetailComponent)
          },
          {
            path: 'customers/:id/payments',
            loadComponent: () => import('./modules/settings/customers/customer-payments.component').then(m => m.CustomerPaymentsComponent)
          },
        ]
      },
    ]
  },
  { path: '**', redirectTo: '' }
];
