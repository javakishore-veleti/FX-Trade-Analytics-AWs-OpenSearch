import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TradeBook } from '../models/trade-book.model';
import { PageResponse } from '../models/page-response.model';

@Injectable({ providedIn: 'root' })
export class TradeBookService {
  private http = inject(HttpClient);
  private base = '/api/master/trade-books';

  list(page: number, size: number, sort?: string): Observable<PageResponse<TradeBook>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (sort) params = params.set('sort', sort);
    return this.http.get<PageResponse<TradeBook>>(this.base, { params });
  }

  get(id: number): Observable<TradeBook> {
    return this.http.get<TradeBook>(`${this.base}/${id}`);
  }

  create(b: TradeBook): Observable<TradeBook> {
    return this.http.post<TradeBook>(this.base, b);
  }

  update(id: number, b: TradeBook): Observable<TradeBook> {
    return this.http.put<TradeBook>(`${this.base}/${id}`, b);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
