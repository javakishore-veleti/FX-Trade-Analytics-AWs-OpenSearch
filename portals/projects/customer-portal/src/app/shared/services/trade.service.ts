import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TradeHit, TradePlaceResponse, TradeRequest } from '../models/trade.model';

@Injectable({ providedIn: 'root' })
export class TradeService {
  private http = inject(HttpClient);

  place(req: TradeRequest): Observable<TradePlaceResponse> {
    return this.http.post<TradePlaceResponse>('/api/trades/place', req);
  }

  search(filters: { risk?: string; region?: string; size?: number } = {}): Observable<TradeHit[]> {
    let params = new HttpParams();
    if (filters.risk) params = params.set('risk', filters.risk);
    if (filters.region) params = params.set('region', filters.region);
    params = params.set('size', filters.size ?? 50);
    return this.http.get<TradeHit[]>('/trades/search', { params });
  }
}
