import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CurrencyPair } from '../models/currency-pair.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({ providedIn: 'root' })
export class CurrencyPairService {
  private http = inject(HttpClient);
  private base = '/api/master/currency-pairs';

  list(page: number, size: number, sort?: string): Observable<PageResponse<CurrencyPair>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (sort) params = params.set('sort', sort);
    return this.http.get<PageResponse<CurrencyPair>>(this.base, { params });
  }

  get(id: number): Observable<CurrencyPair> {
    return this.http.get<CurrencyPair>(`${this.base}/${id}`);
  }

  create(p: CurrencyPair): Observable<CurrencyPair> {
    return this.http.post<CurrencyPair>(this.base, p);
  }

  update(id: number, p: CurrencyPair): Observable<CurrencyPair> {
    return this.http.put<CurrencyPair>(`${this.base}/${id}`, p);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
