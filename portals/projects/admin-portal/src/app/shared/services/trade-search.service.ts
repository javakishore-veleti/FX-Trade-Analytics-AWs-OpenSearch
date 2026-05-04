import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Trade } from '../models/trade.model';

export type SearchMode = 'cross-region' | 'specific-regions';

export interface SearchOptions {
  mode: SearchMode;
  /** Single region (legacy) — only used when mode === 'specific-regions' AND only one region. */
  region?: string;
  /** Comma-separated list when mode === 'specific-regions' (1+ regions). */
  regions?: string[];
  risk?: string;
  /** Filter to a specific trading book (matched against the trade's traderBook field). */
  traderBook?: string;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class TradeSearchService {
  private http = inject(HttpClient);
  private base = '/api/trades-search';   // proxied to fx-trade-service /trades/search

  search(opts: SearchOptions): Observable<Trade[]> {
    let params = new HttpParams();
    if (opts.mode === 'cross-region') {
      params = params.set('crossRegion', 'true');
    } else if (opts.regions && opts.regions.length > 0) {
      params = params.set('regions', opts.regions.join(','));
    } else if (opts.region) {
      params = params.set('region', opts.region);
    }
    if (opts.risk) params = params.set('risk', opts.risk);
    if (opts.traderBook) params = params.set('traderBook', opts.traderBook);
    if (opts.size) params = params.set('size', String(opts.size));
    return this.http.get<Trade[]>(this.base, { params });
  }
}
