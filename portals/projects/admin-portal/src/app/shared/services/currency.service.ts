import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Currency } from '../models/currency.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({ providedIn: 'root' })
export class CurrencyService {
  private http = inject(HttpClient);
  private base = '/api/master/currencies';

  list(page: number, size: number, sort?: string): Observable<PageResponse<Currency>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (sort) params = params.set('sort', sort);
    return this.http.get<PageResponse<Currency>>(this.base, { params });
  }

  get(code: string): Observable<Currency> {
    return this.http.get<Currency>(`${this.base}/${code}`);
  }

  create(c: Currency): Observable<Currency> {
    return this.http.post<Currency>(this.base, c);
  }

  update(code: string, c: Currency): Observable<Currency> {
    return this.http.put<Currency>(`${this.base}/${code}`, c);
  }

  delete(code: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${code}`);
  }
}
