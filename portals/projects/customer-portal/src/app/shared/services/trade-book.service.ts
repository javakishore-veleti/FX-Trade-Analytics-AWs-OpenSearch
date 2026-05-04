import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { PageResponse, TradeBook } from '../models/trade-book.model';

@Injectable({ providedIn: 'root' })
export class TradeBookService {
  private http = inject(HttpClient);
  private base = '/api/master/trade-books';

  /** Fetch every book in one call (200 is plenty for masterdata seed). */
  listAll(): Observable<TradeBook[]> {
    const params = new HttpParams().set('page', 0).set('size', 200).set('sort', 'region,asc');
    return this.http.get<PageResponse<TradeBook>>(this.base, { params }).pipe(map(r => r.content ?? []));
  }
}
