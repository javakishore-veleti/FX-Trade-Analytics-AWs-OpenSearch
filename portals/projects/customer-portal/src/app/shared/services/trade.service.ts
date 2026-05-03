import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TradeHit, TradePlaceResponse, TradeRequest } from '../models/trade.model';

@Injectable({ providedIn: 'root' })
export class TradeService {
  private http = inject(HttpClient);

  /**
   * @param baseUrl Optional region-specific backend URL. Empty string falls back
   *                to the relative path served via proxy.conf.json (local dev).
   */
  place(req: TradeRequest, baseUrl: string = ''): Observable<TradePlaceResponse> {
    return this.http.post<TradePlaceResponse>(`${baseUrl}/api/trades/place`, req);
  }

  search(filters: { risk?: string; region?: string; size?: number } = {}, baseUrl: string = ''): Observable<TradeHit[]> {
    let params = new HttpParams();
    if (filters.risk) params = params.set('risk', filters.risk);
    if (filters.region) params = params.set('region', filters.region);
    params = params.set('size', filters.size ?? 50);
    return this.http.get<TradeHit[]>(`${baseUrl}/trades/search`, { params });
  }
}
