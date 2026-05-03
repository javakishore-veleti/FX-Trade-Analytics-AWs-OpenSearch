import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TradeSearchService } from '../../shared/services/trade-search.service';
import { OpenSearchDeploymentService } from '../../shared/services/opensearch-deployment.service';
import { Trade } from '../../shared/models/trade.model';

@Component({
  selector: 'app-trades-search',
  standalone: true,
  imports: [
    CommonModule, DecimalPipe, DatePipe, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatTableModule, MatProgressBarModule,
    MatTooltipModule, MatSnackBarModule,
  ],
  template: `
    <section class="page-card">
      <header class="page-card__header">
        <div>
          <h2 class="page-card__title">Trades — single-region search</h2>
          <p class="page-card__subtitle">
            Single-region query against the trade-service search API
            (<code>GET /trades/search</code>). For cross-region federated search,
            use the AWS OpenSearch UI app.
          </p>
        </div>
      </header>

      <form [formGroup]="form" (ngSubmit)="run()" class="search-form">
        <mat-form-field appearance="outline">
          <mat-label>Region</mat-label>
          <mat-select formControlName="region">
            @for (r of regions(); track r) {
              <mat-option [value]="r">{{ r }}</mat-option>
            }
          </mat-select>
          @if (regions().length === 0) {
            <mat-hint>No active deployments — Sync first under OpenSearch (AWS).</mat-hint>
          }
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Risk level (optional)</mat-label>
          <mat-select formControlName="risk">
            <mat-option [value]="''">— Any —</mat-option>
            <mat-option value="LOW">LOW</mat-option>
            <mat-option value="MEDIUM">MEDIUM</mat-option>
            <mat-option value="HIGH">HIGH</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Page size</mat-label>
          <input matInput type="number" min="1" max="200" formControlName="size" />
        </mat-form-field>

        <button mat-flat-button color="primary" type="submit"
                [disabled]="form.invalid || loading()">
          <mat-icon>search</mat-icon> Search
        </button>
      </form>

      @if (loading()) { <mat-progress-bar mode="indeterminate"></mat-progress-bar> }

      <div class="page-card__body">
        @if (!loading() && hasSearched() && hits().length === 0) {
          <div class="empty">
            <mat-icon>inbox</mat-icon>
            <h3>No trades match those filters</h3>
            <p>Try a different region or relax the risk filter.</p>
          </div>
        }

        @if (hits().length > 0) {
          <div class="result-meta">
            <span class="pill pill--info">{{ hits().length }} hits</span>
            <span class="result-meta__sep">·</span>
            <span>Region <strong>{{ form.value.region }}</strong></span>
            @if (form.value.risk) {
              <span class="result-meta__sep">·</span>
              <span>Risk <strong>{{ form.value.risk }}</strong></span>
            }
          </div>

          <table mat-table [dataSource]="hits()">
            <ng-container matColumnDef="tradeId">
              <th mat-header-cell *matHeaderCellDef>Trade ID</th>
              <td mat-cell *matCellDef="let t" class="mono">{{ shortId(t) }}</td>
            </ng-container>
            <ng-container matColumnDef="pair">
              <th mat-header-cell *matHeaderCellDef>Pair</th>
              <td mat-cell *matCellDef="let t">
                <span class="code-badge">{{ t.fromCurrency }} → {{ t.toCurrency }}</span>
              </td>
            </ng-container>
            <ng-container matColumnDef="amount">
              <th mat-header-cell *matHeaderCellDef style="text-align: right">From amount</th>
              <td mat-cell *matCellDef="let t" style="text-align: right" class="num">
                {{ t.fromAmount | number:'1.2-2' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="rate">
              <th mat-header-cell *matHeaderCellDef style="text-align: right">Rate</th>
              <td mat-cell *matCellDef="let t" style="text-align: right" class="num">
                {{ t.rate | number:'1.4-4' }}
              </td>
            </ng-container>
            <ng-container matColumnDef="risk">
              <th mat-header-cell *matHeaderCellDef>Risk</th>
              <td mat-cell *matCellDef="let t">
                <span class="pill"
                      [class.pill--ok]="t.riskLevel === 'LOW'"
                      [class.pill--info]="t.riskLevel === 'MEDIUM'"
                      [class.pill--warn]="t.riskLevel === 'HIGH'">
                  {{ t.riskLevel }}
                </span>
              </td>
            </ng-container>
            <ng-container matColumnDef="region">
              <th mat-header-cell *matHeaderCellDef>Region</th>
              <td mat-cell *matCellDef="let t" class="mono">{{ t.region }}</td>
            </ng-container>
            <ng-container matColumnDef="book">
              <th mat-header-cell *matHeaderCellDef>Book</th>
              <td mat-cell *matCellDef="let t" class="mono">{{ t.traderBook }}</td>
            </ng-container>
            <ng-container matColumnDef="timestamp">
              <th mat-header-cell *matHeaderCellDef>Time</th>
              <td mat-cell *matCellDef="let t" class="cell-time">
                {{ formatTimestamp(t.timestamp) | date:'medium' }}
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
    .search-form {
      display: flex; flex-wrap: wrap; gap: 16px; align-items: flex-start;
      padding: 16px 24px 8px 24px;
    }
    .search-form mat-form-field { min-width: 180px; }
    .search-form button { margin-top: 8px; height: 48px; }
    .result-meta {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 24px 16px 24px;
      font-size: 13px;
      color: var(--text-muted);
    }
    .result-meta__sep { color: var(--text-subtle); }
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
    .mono { font-family: 'SF Mono', 'Menlo', monospace; font-size: 12px; color: var(--text-muted); }
    .num  { font-family: 'SF Mono', 'Menlo', monospace; font-variant-numeric: tabular-nums; }
    .cell-time { font-size: 12px; color: var(--text-muted); }
    .pill--info { background: #DBEAFE; color: #1E40AF; }
    .pill--warn { background: #FEF3C7; color: #92400E; }
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
  `],
})
export class TradesSearchComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(TradeSearchService);
  private deployments = inject(OpenSearchDeploymentService);
  private snack = inject(MatSnackBar);

  displayed = ['tradeId', 'pair', 'amount', 'rate', 'risk', 'region', 'book', 'timestamp'];

  hits = signal<Trade[]>([]);
  loading = signal(false);
  hasSearched = signal(false);
  regions = signal<string[]>([]);

  form = this.fb.group({
    region: ['', Validators.required],
    risk: [''],
    size: [50, [Validators.required, Validators.min(1), Validators.max(200)]],
  });

  ngOnInit() {
    // Region dropdown is populated from active OpenSearch deployments.
    this.deployments.list().subscribe({
      next: deps => {
        const activeRegions = Array.from(new Set(
          deps.filter(d => d.status === 'ACTIVE').map(d => d.region)));
        this.regions.set(activeRegions);
        if (activeRegions.length && !this.form.value.region) {
          this.form.patchValue({ region: activeRegions[0] });
        }
      },
      error: () => { /* leave dropdown empty + hint will explain */ },
    });
  }

  run() {
    const v = this.form.getRawValue();
    if (!v.region) return;
    this.loading.set(true);
    this.hasSearched.set(true);
    this.api.search({
      region: v.region,
      risk: v.risk || undefined,
      size: v.size ?? 50,
    }).subscribe({
      next: rows => {
        this.hits.set(rows);
        this.loading.set(false);
      },
      error: e => {
        this.loading.set(false);
        this.snack.open('Search failed: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 });
      },
    });
  }

  shortId(t: Trade): string {
    const id = (t.tradeId ?? t._id ?? '') as string;
    return id ? id.substring(0, 8) + '…' : '—';
  }

  formatTimestamp(t: unknown): Date | null {
    if (t == null) return null;
    if (typeof t === 'number') return new Date(t);
    if (typeof t === 'string') return new Date(t);
    return null;
  }
}
