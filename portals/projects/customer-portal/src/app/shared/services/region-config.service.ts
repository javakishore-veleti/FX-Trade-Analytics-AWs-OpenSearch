import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, ReplaySubject, shareReplay, tap } from 'rxjs';

export interface RegionEndpoints {
  [region: string]: string;
}

@Injectable({ providedIn: 'root' })
export class RegionConfigService {
  private http = inject(HttpClient);
  private cache$ = new ReplaySubject<RegionEndpoints>(1);
  private loaded = false;
  private latest: RegionEndpoints = {};

  /**
   * Fetches the region → backend URL map from trade-service. Cached for the
   * lifetime of the page. The proxy.conf.json forwards /api/config to :9080
   * during local dev.
   */
  load(): Observable<RegionEndpoints> {
    if (!this.loaded) {
      this.loaded = true;
      this.http.get<RegionEndpoints>('/api/config/regions')
        .pipe(
          tap(map => { this.latest = map ?? {}; }),
          shareReplay(1),
        )
        .subscribe({
          next: map => this.cache$.next(map ?? {}),
          error: err => {
            // Fail open: empty map is interpreted by callers as "no region routing"
            // so they fall back to the default base URL via the proxy.
            console.warn('Failed to load region endpoints; falling back to proxy default', err);
            this.cache$.next({});
            this.loaded = true;
          },
        });
    }
    return this.cache$.asObservable();
  }

  /** Returns the absolute backend base URL for the given region, or '' to use the proxy default. */
  endpointFor(region: string | null | undefined): string {
    if (!region) return '';
    return this.latest[region] ?? '';
  }
}
