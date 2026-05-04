import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPairService } from '../../shared/services/currency-pair.service';
import { TradeService } from '../../shared/services/trade.service';
import { RegionConfigService } from '../../shared/services/region-config.service';
import { TradeBookService } from '../../shared/services/trade-book.service';
import { CurrencyPair } from '../../shared/models/currency-pair.model';
import { TradeBook } from '../../shared/models/trade-book.model';

@Component({
  selector: 'app-place-trade',
  standalone: true,
  imports: [
    DecimalPipe, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatSnackBarModule,
  ],
  template: `
    <section class="hero">
      <div class="hero__eyebrow">Foreign Exchange · Live</div>
      <h1 class="hero__title">Place a Trade</h1>
      <p class="hero__sub">Select a currency pair, enter your amount, and route it through our real-time risk + indexing pipeline.</p>
      <div class="hero__actions">
        <button mat-flat-button color="accent"
                class="hero__demo-btn"
                [disabled]="demoBusy() || activePairs().length === 0 || regionCodes().length === 0"
                (click)="generateDemoTrades()">
          @if (demoBusy()) {
            <ng-container>
              <mat-spinner diameter="16" strokeWidth="3"></mat-spinner>
              <span>Generating {{ demoProgress() }}/{{ demoTotal() }} …</span>
            </ng-container>
          } @else {
            <ng-container>
              <mat-icon>auto_fix_high</mat-icon>
              <span>Generate demo trades</span>
            </ng-container>
          }
        </button>
      </div>
    </section>

    <div class="layout">
      <section class="section-card form-card">
        <h3 class="section-title"><mat-icon style="color: var(--brand-teal)">tune</mat-icon> Trade details</h3>

        <form [formGroup]="form">
          <div class="field-block">
            <label class="field-label">Currency pair</label>
            @if (loadingPairs()) {
              <div class="pair-grid">
                @for (i of [1,2,3,4,5,6]; track i) {
                  <div class="pair-card pair-card--skeleton"></div>
                }
              </div>
            } @else if (activePairs().length === 0) {
              <div class="empty-pairs">
                <mat-icon>schedule</mat-icon>
                <div>
                  <strong>No tradable pairs available right now.</strong>
                  <div>Please check back in a moment, or contact your account manager.</div>
                </div>
              </div>
            } @else {
              <div class="pair-grid">
                @for (p of activePairs(); track p.id) {
                  <button type="button"
                          class="pair-card"
                          [class.pair-card--active]="form.value.pairId === p.id"
                          (click)="selectPair(p)">
                    <span class="pair-card__from">{{ p.fromCurrency }}</span>
                    <mat-icon>arrow_forward</mat-icon>
                    <span class="pair-card__to">{{ p.toCurrency }}</span>
                  </button>
                }
              </div>
            }
          </div>

          <div class="field-block">
            <label class="field-label">Amount</label>
            <div class="amount-input">
              <span class="amount-input__currency">{{ form.value.fromCurrency || '—' }}</span>
              <input type="number" min="0" step="0.01" formControlName="fromAmount" class="amount-input__field" />
            </div>
          </div>

          <div class="form-row">
            <mat-form-field appearance="outline">
              <mat-label>Rate</mat-label>
              <input matInput type="number" min="0" step="0.0001" formControlName="rate" />
              <span matSuffix style="color: var(--text-muted); font-size: 12px; margin-right: 8px">
                {{ form.value.fromCurrency }} → {{ form.value.toCurrency }}
              </span>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Region</mat-label>
              <mat-select formControlName="region" (selectionChange)="onRegionChange($event.value)">
                @for (r of regionCodes(); track r) {
                  <mat-option [value]="r">{{ r }} — {{ regionLabel(r) }}</mat-option>
                }
              </mat-select>
              @if (currentRegionUrl()) {
                <mat-hint>Routing to {{ currentRegionUrl() }}</mat-hint>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Trader Book</mat-label>
              <input matInput formControlName="traderBook" />
            </mat-form-field>
          </div>
        </form>
      </section>

      <section class="section-card preview-card">
        <h3 class="section-title"><mat-icon style="color: var(--brand-indigo)">receipt</mat-icon> Order preview</h3>

        <div class="preview-row">
          <span class="preview-row__label">You send</span>
          <span class="preview-row__value">
            <strong>{{ form.value.fromAmount | number:'1.2-2' }}</strong>
            <small>{{ form.value.fromCurrency || '—' }}</small>
          </span>
        </div>
        <div class="preview-row">
          <span class="preview-row__label">Rate applied</span>
          <span class="preview-row__value">
            1 {{ form.value.fromCurrency || '—' }} = <strong>{{ form.value.rate | number:'1.2-4' }}</strong> {{ form.value.toCurrency || '—' }}
          </span>
        </div>
        <div class="preview-row preview-row--accent">
          <span class="preview-row__label">You receive (estimated)</span>
          <span class="preview-row__value">
            <strong class="big">{{ estimated() | number:'1.2-2' }}</strong>
            <small>{{ form.value.toCurrency || '—' }}</small>
          </span>
        </div>

        <div class="route-hint">
          <mat-icon>bolt</mat-icon>
          Routes through Kafka → Risk → OpenSearch indexer
        </div>

        <button mat-flat-button color="primary"
                class="cta"
                [disabled]="form.invalid || submitting()"
                (click)="submit()">
          @if (submitting()) {
            <mat-spinner diameter="18" strokeWidth="3"></mat-spinner>
          } @else {
            <mat-icon>send</mat-icon>
          }
          <span>{{ submitting() ? 'Submitting…' : 'Place Trade' }}</span>
        </button>
      </section>
    </div>
  `,
  styles: [`
    .layout {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 380px;
      gap: 24px;
    }
    @media (max-width: 920px) {
      .layout { grid-template-columns: 1fr; }
    }

    .form-card { padding: 28px; }

    .field-block { margin-bottom: 20px; }
    .field-label {
      display: block;
      font-size: 12px; font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.06em;
      margin-bottom: 10px;
    }

    .pair-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
      gap: 10px;
    }
    .pair-card {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 14px 16px;
      background: var(--surface-card);
      border: 2px solid var(--border-subtle);
      border-radius: 12px;
      font-family: 'SF Mono', 'Menlo', monospace;
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      transition: all 120ms ease;
      color: var(--text-default);
    }
    .pair-card mat-icon { color: var(--text-subtle); font-size: 18px; width: 18px; height: 18px; }
    .pair-card:hover {
      border-color: var(--brand-teal);
      box-shadow: 0 4px 12px rgba(13, 148, 136, 0.12);
    }
    .pair-card--active {
      border-color: var(--brand-teal);
      background: var(--mat-sys-primary-container);
    }
    .pair-card--active mat-icon { color: var(--brand-teal); }
    .pair-card__from { color: var(--brand-teal); }
    .pair-card__to   { color: var(--brand-indigo); }

    .pair-card--skeleton {
      background: linear-gradient(90deg, var(--surface-soft) 0%, #FFFFFF 50%, var(--surface-soft) 100%);
      background-size: 200% 100%;
      animation: shimmer 1.4s linear infinite;
      cursor: default;
      height: 50px;
    }
    @keyframes shimmer {
      0%   { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }

    .empty-pairs {
      display: flex; align-items: flex-start; gap: 12px;
      padding: 16px;
      background: #FEF3C7;
      color: #92400E;
      border-radius: 12px;
      font-size: 13px;
    }
    .empty-pairs mat-icon { color: #B45309; }
    .empty-pairs a { color: #92400E; font-weight: 600; }

    .amount-input {
      display: flex; align-items: center;
      background: var(--surface-soft);
      border: 2px solid var(--border-subtle);
      border-radius: 12px;
      padding: 8px 16px;
      transition: border-color 120ms ease;
    }
    .amount-input:focus-within {
      border-color: var(--brand-teal);
      background: var(--surface-card);
    }
    .amount-input__currency {
      font-family: 'SF Mono', 'Menlo', monospace;
      font-weight: 700;
      font-size: 14px;
      color: var(--brand-teal);
      padding-right: 12px;
      border-right: 1px solid var(--border-subtle);
      margin-right: 12px;
    }
    .amount-input__field {
      flex: 1;
      border: 0; outline: 0; background: transparent;
      font-family: 'Inter', sans-serif;
      font-size: 22px; font-weight: 700;
      color: var(--text-strong);
      letter-spacing: -0.02em;
      font-variant-numeric: tabular-nums;
    }

    /* Preview side */
    .preview-card { padding: 24px; align-self: start; }
    .preview-row {
      display: flex; justify-content: space-between; align-items: baseline;
      padding: 12px 0;
      border-bottom: 1px dashed var(--border-subtle);
    }
    .preview-row:last-of-type { border-bottom: none; }
    .preview-row__label { font-size: 13px; color: var(--text-muted); font-weight: 500; }
    .preview-row__value { font-size: 14px; color: var(--text-default); font-variant-numeric: tabular-nums; }
    .preview-row__value small { color: var(--text-muted); margin-left: 4px; font-weight: 500; }
    .preview-row__value .big { font-size: 24px; color: var(--brand-teal); letter-spacing: -0.02em; }
    .preview-row--accent {
      background: linear-gradient(135deg, var(--surface-soft) 0%, #DBEAFE 100%);
      border-radius: 12px;
      padding: 14px 16px;
      margin-top: 8px;
      border-bottom: none;
    }

    .route-hint {
      display: flex; align-items: center; gap: 8px;
      margin: 16px 0;
      padding: 10px 12px;
      background: #EEF2FF;
      color: #4338CA;
      border-radius: 10px;
      font-size: 12px;
      font-weight: 500;
    }
    .route-hint mat-icon { font-size: 16px; width: 16px; height: 16px; }

    .cta {
      width: 100%;
      height: 48px;
      font-weight: 600;
      font-size: 15px;
      border-radius: 12px !important;
      letter-spacing: 0.01em;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .hero__actions {
      margin-top: 16px;
      display: flex;
      gap: 12px;
    }
    .hero__demo-btn {
      display: inline-flex; align-items: center; gap: 8px;
      font-weight: 600;
      border-radius: 10px !important;
    }
  `],
})
export class PlaceTradeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private pairs = inject(CurrencyPairService);
  private trades = inject(TradeService);
  private regionConfig = inject(RegionConfigService);
  private bookService = inject(TradeBookService);
  private snack = inject(MatSnackBar);

  // Friendly labels for known AWS regions; unknown codes show the code itself.
  private static readonly REGION_LABELS: Record<string, string> = {
    'us-east-1':  'N. Virginia',
    'us-east-2':  'Ohio',
    'us-west-1':  'N. California',
    'us-west-2':  'Oregon',
    'eu-west-1':  'Ireland',
    'eu-west-2':  'London',
    'eu-central-1': 'Frankfurt',
    'ap-south-1': 'Mumbai',
    'ap-southeast-1': 'Singapore',
    'ap-southeast-2': 'Sydney',
    'ap-northeast-1': 'Tokyo',
  };

  activePairs = signal<CurrencyPair[]>([]);
  loadingPairs = signal(true);
  submitting = signal(false);

  // Demo trade generator state
  demoBusy = signal(false);
  demoTotal = signal(0);
  demoProgress = signal(0);

  /** Region → backend URL map fetched from /api/config/regions. */
  regions = signal<Record<string, string>>({});
  regionCodes = computed(() => Object.keys(this.regions()));

  /** All active trade books, fetched once from masterdata. */
  allBooks = signal<TradeBook[]>([]);
  /** Map: region → list of ACTIVE books in that region (precomputed for fast lookup). */
  booksByRegion = computed(() => {
    const map: Record<string, TradeBook[]> = {};
    for (const b of this.allBooks()) {
      if (!b.active || !b.region) continue;
      (map[b.region] ??= []).push(b);
    }
    return map;
  });

  form = this.fb.group({
    pairId: [null as number | null, Validators.required],
    fromCurrency: ['', Validators.required],
    toCurrency: ['', Validators.required],
    fromAmount: [100, [Validators.required, Validators.min(0.01)]],
    rate: [83, [Validators.required, Validators.min(0.0001)]],
    region: ['', Validators.required],
    traderBook: ['FX-BOOK'],
  });

  estimated = computed(() => {
    const v = this.form.value;
    return (v.fromAmount ?? 0) * (v.rate ?? 0);
  });

  /** URL the next trade will be POSTed to (shown as a hint under the region select). */
  currentRegionUrl = signal<string>('');

  ngOnInit() {
    this.pairs.listActive().subscribe({
      next: r => {
        this.activePairs.set((r.content ?? []).filter(p => p.active));
        this.loadingPairs.set(false);
      },
      error: e => {
        this.loadingPairs.set(false);
        this.snack.open('Failed to load pairs: ' + e.message, 'OK', { duration: 4000 });
      },
    });

    this.regionConfig.load().subscribe(map => {
      this.regions.set(map);
      const codes = Object.keys(map);
      // Default to first region if none selected yet
      if (codes.length && !this.form.value.region) {
        this.form.patchValue({ region: codes[0] });
        this.currentRegionUrl.set(map[codes[0]] ?? '');
        // After region default, also default the trader book if books are loaded
        this.assignDefaultBookForRegion(codes[0]);
      }
    });

    this.bookService.listAll().subscribe({
      next: bs => {
        this.allBooks.set(bs);
        // Books arrived after region was already set — backfill the book default now
        const r = this.form.value.region;
        if (r) this.assignDefaultBookForRegion(r);
      },
      error: () => { /* keep traderBook field's default if masterdata is unavailable */ },
    });
  }

  /**
   * Picks the first ACTIVE book whose region matches and writes its code into
   * form.traderBook. No-op if there are no books for that region (the field
   * keeps whatever the user had typed, or the original default).
   */
  private assignDefaultBookForRegion(region: string): void {
    const candidates = this.booksByRegion()[region] ?? [];
    if (candidates.length > 0) {
      this.form.patchValue({ traderBook: candidates[0].code });
    }
  }

  selectPair(p: CurrencyPair) {
    this.form.patchValue({ pairId: p.id, fromCurrency: p.fromCurrency, toCurrency: p.toCurrency });
  }

  onRegionChange(region: string) {
    this.currentRegionUrl.set(this.regions()[region] ?? '');
    this.assignDefaultBookForRegion(region);
  }

  regionLabel(code: string): string {
    return PlaceTradeComponent.REGION_LABELS[code] ?? code;
  }

  submit() {
    const v = this.form.getRawValue();
    if (!v.fromCurrency || !v.toCurrency || !v.region) return;
    this.submitting.set(true);
    const baseUrl = this.regionConfig.endpointFor(v.region);
    this.trades.place({
      fromCurrency: v.fromCurrency,
      toCurrency: v.toCurrency,
      fromAmount: v.fromAmount!,
      rate: v.rate!,
      region: v.region!,
      traderBook: v.traderBook ?? undefined,
    }, baseUrl).subscribe({
      next: r => {
        this.submitting.set(false);
        const where = baseUrl ? ` (${baseUrl})` : '';
        const msg = r.accepted
          ? `Trade queued · ${r.tradeId.substring(0, 8)}…${where}`
          : `Rejected: ${r.reason}`;
        this.snack.open(msg, 'OK', { duration: 4000 });
      },
      error: e => {
        this.submitting.set(false);
        const msg = e.error?.reason ?? e.error?.message ?? e.message;
        this.snack.open('Place failed: ' + msg, 'OK', { duration: 5000 });
      },
    });
  }

  /**
   * Fires N random trades cycling through every configured region. Saves
   * click-fatigue when populating multiple AWS OpenSearch domains for the
   * cross-region UI demo. Shows live progress on the button.
   */
  generateDemoTrades() {
    const pairs = this.activePairs();
    const regions = this.regionCodes();
    if (pairs.length === 0 || regions.length === 0) return;

    const input = window.prompt('How many demo trades? (1–500)', '50');
    if (input == null) return;
    const n = Math.max(1, Math.min(500, Math.floor(Number(input)) || 0));
    if (!n) return;

    this.demoBusy.set(true);
    this.demoTotal.set(n);
    this.demoProgress.set(0);

    let succeeded = 0;
    let rejected = 0;
    let i = 0;
    const fire = () => {
      if (i >= n) {
        this.demoBusy.set(false);
        const msg = `Generated ${succeeded} trade(s) across ${regions.length} region(s)`
                  + (rejected ? `; ${rejected} rejected by master-data validation` : '');
        this.snack.open(msg, 'OK', { duration: 6000 });
        return;
      }
      const pair = pairs[i % pairs.length];
      const region = regions[i % regions.length];
      const baseUrl = this.regionConfig.endpointFor(region);
      const fromAmount = Math.round((50 + Math.random() * 9950) * 100) / 100;
      const rate = Math.round((0.5 + Math.random() * 100) * 10000) / 10000;

      // Pick a book that lives in THIS region. If multiple, round-robin
      // through them so demo data is spread across books too. Falls back
      // to a generic 'DEMO-{n}' string if masterdata had no books for the
      // region (shouldn't happen with the seeded set, but defensive).
      const regionBooks = this.booksByRegion()[region] ?? [];
      const book = regionBooks.length > 0
        ? regionBooks[i % regionBooks.length].code
        : 'DEMO-' + (1 + (i % 3));

      this.trades.place({
        fromCurrency: pair.fromCurrency,
        toCurrency: pair.toCurrency,
        fromAmount,
        rate,
        region,
        traderBook: book,
      }, baseUrl).subscribe({
        next: r => {
          if (r.accepted) succeeded++; else rejected++;
          i++;
          this.demoProgress.set(i);
          // Small delay so the indexer doesn't see a tidal wave + so the
          // browser stays responsive. 80ms = ~12 trades/sec.
          setTimeout(fire, 80);
        },
        error: () => {
          rejected++;
          i++;
          this.demoProgress.set(i);
          setTimeout(fire, 80);
        },
      });
    };
    fire();
  }
}
