import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'currencies' },
  {
    path: 'currencies',
    loadComponent: () => import('./features/currencies/currency-list.component').then(m => m.CurrencyListComponent),
  },
  {
    path: 'currency-pairs',
    loadComponent: () => import('./features/currency-pairs/currency-pair-list.component').then(m => m.CurrencyPairListComponent),
  },
  {
    path: 'trade-books',
    loadComponent: () => import('./features/trade-books/trade-book-list.component').then(m => m.TradeBookListComponent),
  },
  {
    path: 'opensearch-deployments',
    loadComponent: () => import('./features/opensearch-deployments/opensearch-deployments-list.component')
      .then(m => m.OpenSearchDeploymentsListComponent),
  },
  { path: '**', redirectTo: 'currencies' },
];
