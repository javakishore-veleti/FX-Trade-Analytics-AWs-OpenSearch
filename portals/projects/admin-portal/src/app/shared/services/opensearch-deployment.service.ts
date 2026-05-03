import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  DashboardInstallResult,
  OpenSearchDeployment,
  OpenSearchSyncResult,
} from '../models/opensearch-deployment.model';

@Injectable({ providedIn: 'root' })
export class OpenSearchDeploymentService {
  private http = inject(HttpClient);
  private base = '/api/admin/opensearch-deployments';

  list(): Observable<OpenSearchDeployment[]> {
    return this.http.get<OpenSearchDeployment[]>(this.base);
  }

  syncRegion(region: string): Observable<OpenSearchSyncResult> {
    const params = new HttpParams().set('region', region);
    return this.http.post<OpenSearchSyncResult>(`${this.base}/sync`, null, { params });
  }

  syncAll(): Observable<OpenSearchSyncResult> {
    return this.http.post<OpenSearchSyncResult>(`${this.base}/sync-all`, null);
  }

  installDashboards(deploymentId: number): Observable<DashboardInstallResult> {
    return this.http.post<DashboardInstallResult>(`${this.base}/${deploymentId}/install-dashboards`, null);
  }
}
