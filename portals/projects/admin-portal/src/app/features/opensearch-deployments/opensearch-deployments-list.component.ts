import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatMenuModule } from '@angular/material/menu';
import { OpenSearchDeploymentService } from '../../shared/services/opensearch-deployment.service';
import { OpenSearchDeployment } from '../../shared/models/opensearch-deployment.model';

@Component({
  selector: 'app-opensearch-deployments-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule, MatButtonModule, MatIconModule, MatTooltipModule,
    MatProgressBarModule, MatSnackBarModule, MatChipsModule, MatMenuModule,
  ],
  template: `
    <section class="stat-row">
      <div class="stat-tile">
        <div class="stat-tile__label">Total deployments</div>
        <div class="stat-tile__value">{{ rows().length }}</div>
        <div class="stat-tile__hint">tracked in master DB</div>
      </div>
      <div class="stat-tile">
        <div class="stat-tile__label">Active</div>
        <div class="stat-tile__value">{{ activeCount() }}</div>
        <div class="stat-tile__hint">reachable & queryable</div>
      </div>
      <div class="stat-tile">
        <div class="stat-tile__label">Regions covered</div>
        <div class="stat-tile__value">{{ regionsCovered() }}</div>
        <div class="stat-tile__hint">distinct AWS regions</div>
      </div>
      <div class="stat-tile">
        <div class="stat-tile__label">Last sync</div>
        <div class="stat-tile__value stat-tile__value--small">{{ lastSyncedDisplay() }}</div>
        <div class="stat-tile__hint">most recent AWS scan</div>
      </div>
    </section>

    <section class="page-card">
      <header class="page-card__header">
        <div>
          <h2 class="page-card__title">AWS OpenSearch — deployments</h2>
          <p class="page-card__subtitle">
            Discovers managed clusters and serverless collections via the AWS OpenSearch
            control-plane APIs, and stores them as the source of truth used by indexer + search.
          </p>
        </div>
        <span class="spacer" style="flex: 1"></span>
        <button mat-stroked-button color="primary" (click)="syncAll()" [disabled]="syncing()">
          <mat-icon>cloud_sync</mat-icon> Sync all regions
        </button>
      </header>

      @if (loading() || syncing() || installing()) { <mat-progress-bar mode="indeterminate"></mat-progress-bar> }

      <div class="page-card__body">
        @if (!loading() && rows().length === 0) {
          <div class="empty">
            <mat-icon>cloud_off</mat-icon>
            <h3>No deployments tracked yet</h3>
            <p>Click <strong>Sync all regions</strong> to discover AWS OpenSearch domains and collections in the regions configured under <code>fx.aws.regions</code>.</p>
          </div>
        }

        @if (rows().length > 0) {
          <table mat-table [dataSource]="rows()">
            <ng-container matColumnDef="deploymentName">
              <th mat-header-cell *matHeaderCellDef>Deployment</th>
              <td mat-cell *matCellDef="let r">
                <div class="cell-name">{{ r.deploymentName }}</div>
                <div class="cell-sub">{{ r.cloudProvider }} · {{ r.provisionType }}</div>
              </td>
            </ng-container>

            <ng-container matColumnDef="region">
              <th mat-header-cell *matHeaderCellDef>Region</th>
              <td mat-cell *matCellDef="let r"><span class="code-badge">{{ r.region }}</span></td>
            </ng-container>

            <ng-container matColumnDef="provisionType">
              <th mat-header-cell *matHeaderCellDef>Type</th>
              <td mat-cell *matCellDef="let r">
                <span class="pill"
                      [class.pill--ok]="r.provisionType === 'managed'"
                      [class.pill--info]="r.provisionType === 'serverless'"
                      [class.pill--off]="r.provisionType === 'local-docker'">
                  {{ r.provisionType }}
                </span>
              </td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let r">
                <span class="pill"
                      [class.pill--ok]="r.status === 'ACTIVE'"
                      [class.pill--info]="r.status === 'PROCESSING'"
                      [class.pill--warn]="r.status === 'ERROR'"
                      [class.pill--off]="r.status === 'INACTIVE'">
                  {{ r.status }}
                </span>
              </td>
            </ng-container>

            <ng-container matColumnDef="syncedOn">
              <th mat-header-cell *matHeaderCellDef>Last synced</th>
              <td mat-cell *matCellDef="let r" class="cell-time">
                {{ r.syncedOn ? (r.syncedOn | date:'medium') : '—' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef style="text-align: right">Open</th>
              <td mat-cell *matCellDef="let r" style="text-align: right">
                <a mat-icon-button
                   *ngIf="r.dashboardsUrl"
                   [href]="r.dashboardsUrl"
                   target="_blank"
                   rel="noopener"
                   matTooltip="Open OpenSearch Dashboards">
                  <mat-icon>dashboard</mat-icon>
                </a>
                <a mat-icon-button
                   *ngIf="r.awsConsoleUrl"
                   [href]="r.awsConsoleUrl"
                   target="_blank"
                   rel="noopener"
                   matTooltip="Open in AWS Console">
                  <mat-icon>open_in_new</mat-icon>
                </a>
                <button mat-icon-button
                        (click)="syncRegion(r.region)"
                        [disabled]="syncing()"
                        matTooltip="Re-sync this region">
                  <mat-icon>refresh</mat-icon>
                </button>
                <button mat-icon-button
                        (click)="installDashboards(r)"
                        [disabled]="installing() || !r.endpoint || r.status !== 'ACTIVE'"
                        [matTooltip]="!r.endpoint ? 'Sync first to capture endpoint' :
                                       (r.status !== 'ACTIVE' ? 'Deployment must be ACTIVE' :
                                       'Install dashboard templates from codebase')">
                  <mat-icon>auto_awesome</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayed"></tr>
            <tr mat-row *matRowDef="let row; columns: displayed"></tr>
          </table>
        }
      </div>
    </section>
  `,
  styles: [`
    .code-badge {
      display: inline-block;
      font-family: 'SF Mono', 'Menlo', monospace;
      font-weight: 700;
      font-size: 12px;
      color: var(--brand-navy);
      background: var(--mat-sys-primary-container);
      padding: 4px 10px;
      border-radius: 6px;
      letter-spacing: 0.04em;
    }
    .stat-tile__value--small { font-size: 14px; font-weight: 600; }
    .cell-name { font-weight: 600; color: var(--text-strong); }
    .cell-sub  { font-size: 12px; color: var(--text-muted); margin-top: 2px; }
    .cell-time { font-size: 13px; color: var(--text-muted); }
    .empty {
      padding: 48px 24px;
      text-align: center;
      color: var(--text-muted);
    }
    .empty mat-icon {
      font-size: 48px; width: 48px; height: 48px;
      color: var(--text-subtle); margin-bottom: 12px;
    }
    .empty h3 { margin: 0 0 8px 0; color: var(--text-strong); font-weight: 600; }
    .empty p  { margin: 0 auto; max-width: 480px; line-height: 1.5; }
    .empty code {
      background: var(--surface-soft);
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 12px;
    }
    .pill--info {
      background: #DBEAFE;
      color: #1E40AF;
    }
    .pill--warn {
      background: #FEF3C7;
      color: #92400E;
    }
  `],
})
export class OpenSearchDeploymentsListComponent implements OnInit {
  private api = inject(OpenSearchDeploymentService);
  private snack = inject(MatSnackBar);

  displayed = ['deploymentName', 'region', 'provisionType', 'status', 'syncedOn', 'actions'];
  rows = signal<OpenSearchDeployment[]>([]);
  loading = signal(false);
  syncing = signal(false);
  installing = signal(false);

  activeCount = computed(() => this.rows().filter(r => r.status === 'ACTIVE').length);
  regionsCovered = computed(() => new Set(this.rows().map(r => r.region)).size);
  lastSyncedDisplay = computed(() => {
    const stamps = this.rows().map(r => r.syncedOn).filter((s): s is string => !!s);
    if (stamps.length === 0) return '—';
    const latest = stamps.sort().reverse()[0];
    return new Date(latest).toLocaleString();
  });

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.list().subscribe({
      next: rows => { this.rows.set(rows); this.loading.set(false); },
      error: e => {
        this.snack.open('Failed to load deployments: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 });
        this.loading.set(false);
      },
    });
  }

  syncAll() {
    this.syncing.set(true);
    this.api.syncAll().subscribe({
      next: r => {
        this.syncing.set(false);
        const errs = r.errors?.length ? ` (${r.errors.length} error${r.errors.length === 1 ? '' : 's'})` : '';
        this.snack.open(
          `Scanned ${r.regionsScanned} region(s); discovered ${r.deploymentsDiscovered}; updated ${r.updated}${errs}`,
          'OK', { duration: 4000 });
        this.load();
      },
      error: e => {
        this.syncing.set(false);
        this.snack.open('Sync failed: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 });
      },
    });
  }

  syncRegion(region: string) {
    this.syncing.set(true);
    this.api.syncRegion(region).subscribe({
      next: r => {
        this.syncing.set(false);
        const errs = r.errors?.length ? ` (${r.errors.length} error${r.errors.length === 1 ? '' : 's'})` : '';
        this.snack.open(
          `${region}: discovered ${r.deploymentsDiscovered}; updated ${r.updated}${errs}`,
          'OK', { duration: 4000 });
        this.load();
      },
      error: e => {
        this.syncing.set(false);
        this.snack.open(`Sync ${region} failed: ` + (e.error?.message ?? e.message), 'OK', { duration: 5000 });
      },
    });
  }

  installDashboards(d: OpenSearchDeployment) {
    if (!confirm(`Install dashboard templates into ${d.deploymentName} (${d.region})?\n\nNote: AWS managed clusters require Fine-Grained Access Control (FGAC) to accept the import — without it the call will fail with anonymous-not-authorized.`)) {
      return;
    }
    this.installing.set(true);
    this.api.installDashboards(d.id).subscribe({
      next: r => {
        this.installing.set(false);
        if (r.templatesSucceeded === r.templatesAttempted) {
          this.snack.open(
            `${d.deploymentName}: installed ${r.templatesSucceeded}/${r.templatesAttempted} template(s).`,
            'OK', { duration: 4000 });
        } else {
          const failed = r.results.filter(t => !t.ok).map(t => `${t.template}: ${t.message}`).join('; ');
          this.snack.open(
            `${d.deploymentName}: ${r.templatesSucceeded}/${r.templatesAttempted} succeeded. Failed: ${failed}`,
            'OK', { duration: 8000 });
        }
      },
      error: e => {
        this.installing.set(false);
        this.snack.open(
          `Install failed: ` + (e.error?.message ?? e.message),
          'OK', { duration: 6000 });
      },
    });
  }
}
