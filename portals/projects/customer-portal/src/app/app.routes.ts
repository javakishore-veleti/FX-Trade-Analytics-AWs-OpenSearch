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
  { path: '**', redirectTo: 'place' },
];
