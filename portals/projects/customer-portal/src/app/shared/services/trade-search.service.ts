import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type SearchMode = 'cross-region' | 'specific-regions';

export interface SearchOptions {
  mode: SearchMode;
  region?: string;
  regions?: string[];
  risk?: string;
  size?: number;
}

export interface TradeRow {
  _id?: string;
  tradeId?: string;
  traderBook?: string;
  fromCurrency?: string;
  toCurrency?: string;
  fromAmount?: number;
  toAmount?: number;
  rate?: number;
  region?: string;
  riskLevel?: string;
  timestamp?: string | number;
  [key: string]: unknown;
}

@Injectable({ providedIn: 'root' })
export class TradeSearchService {
  private http = inject(HttpClient);
  private base = '/api/trades-search';

  search(opts: SearchOptions): Observable<TradeRow[]> {
    let params = new HttpParams();
    if (opts.mode === 'cross-region') {
      params = params.set('crossRegion', 'true');
    } else if (opts.regions && opts.regions.length > 0) {
      params = params.set('regions', opts.regions.join(','));
    } else if (opts.region) {
      params = params.set('region', opts.region);
    }
    if (opts.risk) params = params.set('risk', opts.risk);
    if (opts.size) params = params.set('size', String(opts.size));
    return this.http.get<TradeRow[]>(this.base, { params });
  }
}
