import { Component, OnInit, signal, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPairService } from '../../shared/services/currency-pair.service';
import { TradeService } from '../../shared/services/trade.service';
import { CurrencyPair } from '../../shared/models/currency-pair.model';

@Component({
  selector: 'app-place-trade',
  standalone: true,
  imports: [
    DecimalPipe, ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatSnackBarModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Place a Trade</mat-card-title>
        <mat-card-subtitle>Pairs and books are sourced from master data</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content style="padding-top: 16px">
        <form [formGroup]="form" class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Currency Pair</mat-label>
            <mat-select formControlName="pairId" (selectionChange)="onPairChange($event.value)">
              @for (p of activePairs(); track p.id) {
                <mat-option [value]="p.id">{{ p.fromCurrency }} → {{ p.toCurrency }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>From Amount</mat-label>
            <input matInput type="number" min="0" step="0.01" formControlName="fromAmount" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Rate</mat-label>
            <input matInput type="number" min="0" step="0.0001" formControlName="rate" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Region</mat-label>
            <mat-select formControlName="region">
              <mat-option value="us-east-1">us-east-1</mat-option>
              <mat-option value="us-west-2">us-west-2</mat-option>
              <mat-option value="eu-west-1">eu-west-1</mat-option>
              <mat-option value="ap-south-1">ap-south-1</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Trader Book</mat-label>
            <input matInput formControlName="traderBook" />
          </mat-form-field>
        </form>

        <p style="opacity: 0.7">
          Estimated to-amount: <strong>{{ estimated() | number:'1.2-4' }}</strong>
        </p>
      </mat-card-content>
      <mat-card-actions align="end">
        <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
          <mat-icon>send</mat-icon> Place Trade
        </button>
      </mat-card-actions>
    </mat-card>
  `,
})
export class PlaceTradeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private pairs = inject(CurrencyPairService);
  private trades = inject(TradeService);
  private snack = inject(MatSnackBar);

  activePairs = signal<CurrencyPair[]>([]);
  submitting = signal(false);

  form = this.fb.group({
    pairId: [null as number | null, Validators.required],
    fromCurrency: ['', Validators.required],
    toCurrency: ['', Validators.required],
    fromAmount: [100, [Validators.required, Validators.min(0.01)]],
    rate: [83, [Validators.required, Validators.min(0.0001)]],
    region: ['us-east-1', Validators.required],
    traderBook: ['FX-BOOK'],
  });

  ngOnInit() {
    this.pairs.listActive().subscribe({
      next: r => this.activePairs.set((r.content ?? []).filter(p => p.active)),
      error: e => this.snack.open('Failed to load pairs: ' + e.message, 'OK', { duration: 4000 }),
    });
  }

  onPairChange(id: number) {
    const p = this.activePairs().find(x => x.id === id);
    if (p) this.form.patchValue({ fromCurrency: p.fromCurrency, toCurrency: p.toCurrency });
  }

  estimated(): number {
    const v = this.form.getRawValue();
    return (v.fromAmount ?? 0) * (v.rate ?? 0);
  }

  submit() {
    const v = this.form.getRawValue();
    if (!v.fromCurrency || !v.toCurrency) return;
    this.submitting.set(true);
    this.trades.place({
      fromCurrency: v.fromCurrency,
      toCurrency: v.toCurrency,
      fromAmount: v.fromAmount!,
      rate: v.rate!,
      region: v.region!,
      traderBook: v.traderBook ?? undefined,
    }).subscribe({
      next: r => {
        this.submitting.set(false);
        this.snack.open(r.accepted ? `Queued: ${r.tradeId}` : `Rejected: ${r.reason}`, 'OK', { duration: 4000 });
      },
      error: e => {
        this.submitting.set(false);
        const body = e.error;
        const msg = body?.reason ?? body?.message ?? e.message;
        this.snack.open('Place failed: ' + msg, 'OK', { duration: 5000 });
      },
    });
  }
}
