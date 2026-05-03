import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TradeService } from '../../shared/services/trade.service';
import { TradeHit } from '../../shared/models/trade.model';

@Component({
  selector: 'app-recent-trades',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe, FormsModule,
    MatTableModule, MatFormFieldModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatProgressBarModule, MatSnackBarModule,
  ],
  template: `
    <header class="page-head">
      <div>
        <h1 class="page-head__title">Recent Trades</h1>
        <p class="page-head__sub">Live view from OpenSearch · sorted by most recent · auto-refresh manual</p>
      </div>
      <button mat-stroked-button color="primary" (click)="load()">
        <mat-icon>refresh</mat-icon> Refresh
      </button>
    </header>

    <section class="stat-row">
      <div class="stat-card">
        <div class="stat-card__label">Total trades</div>
        <div class="stat-card__value">{{ rows().length }}</div>
        <div class="stat-card__hint">in current view</div>
      </div>
      <div class="stat-card stat-card--low">
        <div class="stat-card__label">Low risk</div>
        <div class="stat-card__value">{{ countByRisk('LOW') }}</div>
        <div class="stat-card__hint">{{ pct('LOW') }}% of total</div>
      </div>
      <div class="stat-card stat-card--medium">
        <div class="stat-card__label">Medium risk</div>
        <div class="stat-card__value">{{ countByRisk('MEDIUM') }}</div>
        <div class="stat-card__hint">{{ pct('MEDIUM') }}% of total</div>
      </div>
      <div class="stat-card stat-card--high">
        <div class="stat-card__label">High risk</div>
        <div class="stat-card__value">{{ countByRisk('HIGH') }}</div>
        <div class="stat-card__hint">{{ pct('HIGH') }}% of total</div>
      </div>
    </section>

    <section class="section-card">
      <div class="filter-bar">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width: 180px">
          <mat-label>Risk</mat-label>
          <mat-select [(ngModel)]="risk" (selectionChange)="load()">
            <mat-option [value]="''">All</mat-option>
            <mat-option value="LOW">Low</mat-option>
            <mat-option value="MEDIUM">Medium</mat-option>
            <mat-option value="HIGH">High</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic" style="width: 220px">
          <mat-label>Region</mat-label>
          <mat-select [(ngModel)]="region" (selectionChange)="load()">
            <mat-option [value]="''">All regions</mat-option>
            <mat-option value="us-east-1">us-east-1</mat-option>
            <mat-option value="us-west-2">us-west-2</mat-option>
            <mat-option value="eu-west-1">eu-west-1</mat-option>
            <mat-option value="ap-south-1">ap-south-1</mat-option>
          </mat-select>
        </mat-form-field>
        <span class="spacer" style="flex: 1"></span>
        <span class="result-count">{{ rows().length }} trades shown</span>
      </div>

      @if (loading()) { <mat-progress-bar mode="indeterminate"></mat-progress-bar> }

      @if (!loading() && rows().length === 0) {
        <div class="empty">
          <mat-icon>inbox</mat-icon>
          <h3>No trades yet</h3>
          <p>Place a trade to populate this view, or check that the indexer service is running and the index pattern <code>fx-trades-*</code> exists in OpenSearch.</p>
        </div>
      }

      @if (rows().length > 0) {
        <table mat-table [dataSource]="rows()">
          <ng-container matColumnDef="when">
            <th mat-header-cell *matHeaderCellDef>When</th>
            <td mat-cell *matCellDef="let r" style="color: var(--text-muted)">{{ r.timestamp | date:'short' }}</td>
          </ng-container>
          <ng-container matColumnDef="tradeId">
            <th mat-header-cell *matHeaderCellDef>Trade ID</th>
            <td mat-cell *matCellDef="let r"><code class="id">{{ r.tradeId?.substring(0, 8) }}…</code></td>
          </ng-container>
          <ng-container matColumnDef="pair">
            <th mat-header-cell *matHeaderCellDef>Pair</th>
            <td mat-cell *matCellDef="let r">
              <span class="pair-chip">
                <span class="pair-chip__from">{{ r.fromCurrency }}</span>
                <mat-icon style="font-size: 14px; width: 14px; height: 14px; color: var(--text-subtle)">arrow_forward</mat-icon>
                <span class="pair-chip__to">{{ r.toCurrency }}</span>
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="from">
            <th mat-header-cell *matHeaderCellDef style="text-align: right">From</th>
            <td mat-cell *matCellDef="let r" class="cell-num" style="text-align: right">{{ r.fromAmount | number:'1.2-2' }}</td>
          </ng-container>
          <ng-container matColumnDef="to">
            <th mat-header-cell *matHeaderCellDef style="text-align: right">To</th>
            <td mat-cell *matCellDef="let r" class="cell-num" style="text-align: right">{{ r.toAmount | number:'1.2-2' }}</td>
          </ng-container>
          <ng-container matColumnDef="rate">
            <th mat-header-cell *matHeaderCellDef style="text-align: right">Rate</th>
            <td mat-cell *matCellDef="let r" class="cell-num" style="text-align: right">{{ r.rate | number:'1.2-4' }}</td>
          </ng-container>
          <ng-container matColumnDef="region">
            <th mat-header-cell *matHeaderCellDef>Region</th>
            <td mat-cell *matCellDef="let r"><span class="region-tag">{{ r.region }}</span></td>
          </ng-container>
          <ng-container matColumnDef="risk">
            <th mat-header-cell *matHeaderCellDef>Risk</th>
            <td mat-cell *matCellDef="let r">
              <span class="risk-pill" [class]="'risk-pill risk-pill--' + (r.riskLevel || 'LOW')">{{ r.riskLevel }}</span>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayed"></tr>
          <tr mat-row *matRowDef="let row; columns: displayed"></tr>
        </table>
      }
    </section>
  `,
  styles: [`
    .page-head {
      display: flex; align-items: center; justify-content: space-between;
      margin-bottom: 24px;
    }
    .page-head__title {
      font-size: 24px; font-weight: 700; margin: 0;
      color: var(--text-strong); letter-spacing: -0.02em;
    }
    .page-head__sub { font-size: 13px; color: var(--text-muted); margin: 4px 0 0 0; }

    .filter-bar {
      display: flex; align-items: center; gap: 16px;
      margin-bottom: 16px;
    }
    .result-count { font-size: 13px; color: var(--text-muted); font-weight: 500; }

    .empty {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-muted);
    }
    .empty mat-icon { font-size: 48px; width: 48px; height: 48px; color: var(--text-subtle); }
    .empty h3 { margin: 12px 0 4px 0; color: var(--text-default); font-weight: 600; }
    .empty p { font-size: 14px; max-width: 480px; margin: 0 auto; line-height: 1.5; }
    .empty code {
      background: var(--surface-soft);
      padding: 2px 6px;
      border-radius: 4px;
      font-family: 'SF Mono', 'Menlo', monospace;
      font-size: 12px;
    }

    .id {
      font-family: 'SF Mono', 'Menlo', monospace;
      font-size: 12px;
      color: var(--text-muted);
      background: var(--surface-soft);
      padding: 2px 8px;
      border-radius: 6px;
    }
    .region-tag {
      font-family: 'SF Mono', 'Menlo', monospace;
      font-size: 12px;
      color: var(--brand-indigo);
      background: #EEF2FF;
      padding: 3px 8px;
      border-radius: 6px;
      font-weight: 600;
    }
  `],
})
export class RecentTradesComponent implements OnInit {
  private trades = inject(TradeService);
  private snack = inject(MatSnackBar);

  displayed = ['when', 'tradeId', 'pair', 'from', 'to', 'rate', 'region', 'risk'];
  rows = signal<TradeHit[]>([]);
  loading = signal(false);
  risk = '';
  region = '';

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.trades.search({ risk: this.risk, region: this.region, size: 50 }).subscribe({
      next: r => { this.rows.set(r); this.loading.set(false); },
      error: e => {
        this.loading.set(false);
        this.snack.open('Search failed: ' + e.message, 'OK', { duration: 4000 });
      },
    });
  }

  countByRisk(level: string): number {
    return this.rows().filter(r => r.riskLevel === level).length;
  }

  pct(level: string): number {
    const total = this.rows().length;
    if (total === 0) return 0;
    return Math.round((this.countByRisk(level) / total) * 100);
  }
}
