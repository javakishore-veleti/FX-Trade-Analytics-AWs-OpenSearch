import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Trade } from '../models/trade.model';

@Injectable({ providedIn: 'root' })
export class TradeSearchService {
  private http = inject(HttpClient);
  private base = '/api/trades-search';   // proxied to fx-trade-service /trades/search

  search(opts: { region: string; risk?: string; size?: number }): Observable<Trade[]> {
    let params = new HttpParams().set('region', opts.region);
    if (opts.risk) params = params.set('risk', opts.risk);
    if (opts.size) params = params.set('size', String(opts.size));
    return this.http.get<Trade[]>(this.base, { params });
  }
}
