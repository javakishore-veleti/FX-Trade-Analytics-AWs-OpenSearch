import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'place' },
  {
    path: 'place',
    loadComponent: () => import('./features/place-trade/place-trade.component').then(m => m.PlaceTradeComponent),
  },
  {
    path: 'recent',
    loadComponent: () => import('./features/recent-trades/recent-trades.component').then(m => m.RecentTradesComponent),
  },
  {
    path: 'search',
    loadComponent: () => import('./features/trades-search/trades-search.component').then(m => m.TradesSearchComponent),
  },
  { path: '**', redirectTo: 'place' },
];
