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
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TradeSearchService, SearchMode, TradeRow } from '../../shared/services/trade-search.service';
import { RegionConfigService } from '../../shared/services/region-config.service';

@Component({
  selector: 'app-trades-search',
  standalone: true,
  imports: [
    CommonModule, DecimalPipe, DatePipe, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatRadioModule,
    MatButtonModule, MatIconModule, MatTableModule, MatProgressBarModule,
    MatSnackBarModule,
  ],
  template: `
    <section class="hero">
      <div class="hero__eyebrow">Foreign Exchange · Live</div>
      <h1 class="hero__title">Search Trades</h1>
      <p class="hero__sub">
        Look up trades across one region, several regions, or every region we
        run in — with the source region shown alongside each result.
      </p>
    </section>

    <section class="section-card">
      <form [formGroup]="form" (ngSubmit)="run()" class="search-form">
        <div class="mode-row">
          <span class="mode-label">Search across</span>
          <mat-radio-group formControlName="mode" class="mode-radio">
            <mat-radio-button value="cross-region">Cross-region (all)</mat-radio-button>
            <mat-radio-button value="specific-regions">Specific regions</mat-radio-button>
          </mat-radio-group>
        </div>

        @if (form.value.mode === 'specific-regions') {
          <mat-form-field appearance="outline" class="regions-field">
            <mat-label>Regions (comma-separated)</mat-label>
            <input matInput formControlName="regionsInput"
                   placeholder="us-east-1, eu-west-2, ap-south-1" />
            <mat-hint>
              Available: {{ availableRegions().join(', ') || '(loading from /api/config/regions)' }}
            </mat-hint>
          </mat-form-field>
        }

        <div class="filter-row">
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
                  [disabled]="!canSearch() || loading()">
            <mat-icon>search</mat-icon> Search
          </button>
        </div>
      </form>

      @if (loading()) { <mat-progress-bar mode="indeterminate"></mat-progress-bar> }

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
          <span>Mode <strong>{{ form.value.mode }}</strong></span>
          @if (form.value.mode === 'specific-regions' && form.value.regionsInput) {
            <span class="result-meta__sep">·</span>
            <span>Regions <strong>{{ form.value.regionsInput }}</strong></span>
          }
          @if (form.value.risk) {
            <span class="result-meta__sep">·</span>
            <span>Risk <strong>{{ form.value.risk }}</strong></span>
          }
          <span class="result-meta__sep">·</span>
          <span>Distinct in results: <strong>{{ regionsSeenInResults() }}</strong></span>
        </div>

        <table mat-table [dataSource]="hits()">
          <ng-container matColumnDef="region">
            <th mat-header-cell *matHeaderCellDef>Region</th>
            <td mat-cell *matCellDef="let t">
              <span class="code-badge">{{ t.region }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="tradeId">
            <th mat-header-cell *matHeaderCellDef>Trade ID</th>
            <td mat-cell *matCellDef="let t" class="mono">{{ shortId(t) }}</td>
          </ng-container>
          <ng-container matColumnDef="pair">
            <th mat-header-cell *matHeaderCellDef>Pair</th>
            <td mat-cell *matCellDef="let t">
              <span class="pair">{{ t.fromCurrency }} → {{ t.toCurrency }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="amount">
            <th mat-header-cell *matHeaderCellDef style="text-align: right">Amount</th>
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
    </section>
  `,
  styles: [`
    .section-card { padding: 24px; }
    .search-form {
      display: flex; flex-direction: column; gap: 16px;
    }
    .mode-row {
      display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
    }
    .mode-label {
      font-size: 12px; font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.06em;
    }
    .mode-radio { display: flex; gap: 16px; }
    .regions-field { width: 100%; max-width: 600px; }
    .filter-row {
      display: flex; flex-wrap: wrap; gap: 16px; align-items: flex-start;
    }
    .filter-row mat-form-field { min-width: 180px; }
    .filter-row button { margin-top: 8px; height: 48px; }
    .result-meta {
      display: flex; align-items: center; gap: 8px; flex-wrap: wrap;
      margin: 16px 0 12px 0;
      font-size: 13px;
      color: var(--text-muted);
    }
    .result-meta__sep { color: var(--text-subtle); }
    .code-badge {
      display: inline-block;
      font-family: 'SF Mono', 'Menlo', monospace;
      font-weight: 700;
      font-size: 12px;
      color: var(--brand-teal);
      background: var(--mat-sys-primary-container);
      padding: 4px 10px;
      border-radius: 6px;
      letter-spacing: 0.04em;
    }
    .pair { font-family: 'SF Mono', 'Menlo', monospace; font-weight: 700; font-size: 13px; }
    .mono { font-family: 'SF Mono', 'Menlo', monospace; font-size: 12px; color: var(--text-muted); }
    .num  { font-family: 'SF Mono', 'Menlo', monospace; font-variant-numeric: tabular-nums; }
    .cell-time { font-size: 12px; color: var(--text-muted); }
    .pill {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 11px; font-weight: 600;
      letter-spacing: 0.02em;
    }
    .pill--ok   { background: #ECFDF5; color: #065F46; }
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
  private regionConfig = inject(RegionConfigService);
  private snack = inject(MatSnackBar);

  displayed = ['region', 'tradeId', 'pair', 'amount', 'rate', 'risk', 'timestamp'];

  hits = signal<TradeRow[]>([]);
  loading = signal(false);
  hasSearched = signal(false);
  availableRegions = signal<string[]>([]);

  form = this.fb.group({
    mode: ['cross-region' as SearchMode, Validators.required],
    regionsInput: [''],
    risk: [''],
    size: [50, [Validators.required, Validators.min(1), Validators.max(200)]],
  });

  regionsSeenInResults = computed(() => {
    const set = new Set(this.hits().map(t => t.region).filter(Boolean) as string[]);
    return Array.from(set).join(', ') || '—';
  });

  canSearch = computed(() => {
    const v = this.form.value;
    if (v.mode === 'cross-region') return true;
    return !!(v.regionsInput && v.regionsInput.trim().length > 0);
  });

  ngOnInit() {
    // Region list comes from trade-service /api/config/regions, same source the
    // Place Trade page already uses for its dropdown.
    this.regionConfig.load().subscribe(map => {
      const codes = Object.keys(map);
      this.availableRegions.set(codes);
      if (codes.length && !this.form.value.regionsInput) {
        this.form.patchValue({ regionsInput: codes.join(', ') });
      }
    });
  }

  run() {
    const v = this.form.getRawValue();
    if (!this.canSearch()) return;
    this.loading.set(true);
    this.hasSearched.set(true);

    const regions = v.mode === 'specific-regions' && v.regionsInput
      ? v.regionsInput.split(',').map(r => r.trim()).filter(Boolean)
      : [];

    this.api.search({
      mode: v.mode!,
      regions,
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

  shortId(t: TradeRow): string {
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
