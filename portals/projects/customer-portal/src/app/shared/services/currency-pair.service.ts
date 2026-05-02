import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CurrencyPair, PageResponse } from '../models/currency-pair.model';

@Injectable({ providedIn: 'root' })
export class CurrencyPairService {
  private http = inject(HttpClient);

  listActive(): Observable<PageResponse<CurrencyPair>> {
    const params = new HttpParams().set('page', 0).set('size', 200).set('sort', 'fromCurrency,asc');
    return this.http.get<PageResponse<CurrencyPair>>('/api/master/currency-pairs', { params });
  }
}
