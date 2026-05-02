import { Component, OnInit, signal, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TradeService } from '../../shared/services/trade.service';
import { TradeHit } from '../../shared/models/trade.model';

@Component({
  selector: 'app-recent-trades',
  standalone: true,
  imports: [
    DatePipe, DecimalPipe, FormsModule, MatTableModule, MatFormFieldModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatProgressBarModule, MatChipsModule, MatSnackBarModule,
  ],
  template: `
    <div class="form-row" style="margin-bottom: 12px; align-items: center">
      <mat-form-field appearance="outline" style="flex: 0 1 200px">
        <mat-label>Risk</mat-label>
        <mat-select [(ngModel)]="risk" (selectionChange)="load()">
          <mat-option [value]="''">All</mat-option>
          <mat-option value="LOW">LOW</mat-option>
          <mat-option value="MEDIUM">MEDIUM</mat-option>
          <mat-option value="HIGH">HIGH</mat-option>
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" style="flex: 0 1 200px">
        <mat-label>Region</mat-label>
        <mat-select [(ngModel)]="region" (selectionChange)="load()">
          <mat-option [value]="''">All</mat-option>
          <mat-option value="us-east-1">us-east-1</mat-option>
          <mat-option value="us-west-2">us-west-2</mat-option>
          <mat-option value="eu-west-1">eu-west-1</mat-option>
          <mat-option value="ap-south-1">ap-south-1</mat-option>
        </mat-select>
      </mat-form-field>
      <button mat-stroked-button (click)="load()">
        <mat-icon>refresh</mat-icon> Refresh
      </button>
    </div>

    @if (loading()) { <mat-progress-bar mode="indeterminate"></mat-progress-bar> }
    @if (!loading() && rows().length === 0) {
      <p style="opacity: 0.7">No trades found. (Make sure the indexer is running and the index pattern <code>fx-trades-*</code> has documents.)</p>
    }

    @if (rows().length) {
    <table mat-table [dataSource]="rows()">
      <ng-container matColumnDef="when">
        <th mat-header-cell *matHeaderCellDef>When</th>
        <td mat-cell *matCellDef="let r">{{ r.timestamp | date:'short' }}</td>
      </ng-container>
      <ng-container matColumnDef="tradeId">
        <th mat-header-cell *matHeaderCellDef>Trade ID</th>
        <td mat-cell *matCellDef="let r"><code>{{ r.tradeId }}</code></td>
      </ng-container>
      <ng-container matColumnDef="pair">
        <th mat-header-cell *matHeaderCellDef>Pair</th>
        <td mat-cell *matCellDef="let r">{{ r.fromCurrency }} → {{ r.toCurrency }}</td>
      </ng-container>
      <ng-container matColumnDef="from">
        <th mat-header-cell *matHeaderCellDef>From</th>
        <td mat-cell *matCellDef="let r">{{ r.fromAmount | number:'1.2-2' }}</td>
      </ng-container>
      <ng-container matColumnDef="to">
        <th mat-header-cell *matHeaderCellDef>To</th>
        <td mat-cell *matCellDef="let r">{{ r.toAmount | number:'1.2-2' }}</td>
      </ng-container>
      <ng-container matColumnDef="rate">
        <th mat-header-cell *matHeaderCellDef>Rate</th>
        <td mat-cell *matCellDef="let r">{{ r.rate | number:'1.2-4' }}</td>
      </ng-container>
      <ng-container matColumnDef="region">
        <th mat-header-cell *matHeaderCellDef>Region</th>
        <td mat-cell *matCellDef="let r">{{ r.region }}</td>
      </ng-container>
      <ng-container matColumnDef="risk">
        <th mat-header-cell *matHeaderCellDef>Risk</th>
        <td mat-cell *matCellDef="let r">
          <mat-chip [color]="riskColor(r.riskLevel)" highlighted>{{ r.riskLevel }}</mat-chip>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="displayed"></tr>
      <tr mat-row *matRowDef="let row; columns: displayed"></tr>
    </table>
    }
  `,
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

  riskColor(r?: string): 'primary' | 'accent' | 'warn' {
    if (r === 'HIGH') return 'warn';
    if (r === 'MEDIUM') return 'accent';
    return 'primary';
  }
}
