import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyPairService } from '../../shared/services/currency-pair.service';
import { CurrencyPair } from '../../shared/models/currency-pair.model';
import { CurrencyPairFormComponent } from './currency-pair-form.component';

@Component({
  selector: 'app-currency-pair-list',
  standalone: true,
  imports: [
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule, MatTooltipModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule,
  ],
  template: `
    <section class="stat-row">
      <div class="stat-tile">
        <div class="stat-tile__label">Total pairs</div>
        <div class="stat-tile__value">{{ total() }}</div>
        <div class="stat-tile__hint">configured FX pairs</div>
      </div>
      <div class="stat-tile">
        <div class="stat-tile__label">Active</div>
        <div class="stat-tile__value">{{ activeCount() }}</div>
        <div class="stat-tile__hint">tradable now</div>
      </div>
      <div class="stat-tile">
        <div class="stat-tile__label">Disabled</div>
        <div class="stat-tile__value">{{ total() - activeCount() }}</div>
        <div class="stat-tile__hint">excluded from new trades</div>
      </div>
    </section>

    <section class="page-card">
      <header class="page-card__header">
        <div>
          <h2 class="page-card__title">Currency Pairs</h2>
          <p class="page-card__subtitle">Allowed FX trading combinations — fed to the trade-service allow-list</p>
        </div>
        <span class="spacer" style="flex: 1"></span>
        <button mat-flat-button color="primary" (click)="openForm(null)">
          <mat-icon>add</mat-icon> New Pair
        </button>
      </header>

      @if (loading()) { <mat-progress-bar mode="indeterminate"></mat-progress-bar> }

      <div class="page-card__body">
        <table mat-table [dataSource]="rows()">
          <ng-container matColumnDef="id">
            <th mat-header-cell *matHeaderCellDef>ID</th>
            <td mat-cell *matCellDef="let r" style="color: var(--text-muted)">#{{ r.id }}</td>
          </ng-container>
          <ng-container matColumnDef="pair">
            <th mat-header-cell *matHeaderCellDef>Pair</th>
            <td mat-cell *matCellDef="let r">
              <span class="pair-tile">
                <span class="pair-tile__from">{{ r.fromCurrency }}</span>
                <mat-icon class="pair-tile__arrow">arrow_forward</mat-icon>
                <span class="pair-tile__to">{{ r.toCurrency }}</span>
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="active">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let r">
              <span class="pill" [class.pill--ok]="r.active" [class.pill--off]="!r.active">
                {{ r.active ? 'Active' : 'Inactive' }}
              </span>
            </td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef style="text-align: right"></th>
            <td mat-cell *matCellDef="let r" style="text-align: right">
              <button mat-icon-button (click)="openForm(r)" matTooltip="Edit"><mat-icon>edit</mat-icon></button>
              <button mat-icon-button color="warn" (click)="remove(r)" matTooltip="Delete"><mat-icon>delete_outline</mat-icon></button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="displayed"></tr>
          <tr mat-row *matRowDef="let row; columns: displayed"></tr>
        </table>
      </div>

      <mat-paginator
        [length]="total()"
        [pageSize]="size()"
        [pageIndex]="page()"
        [pageSizeOptions]="[10, 20, 50]"
        (page)="onPage($event)">
      </mat-paginator>
    </section>
  `,
  styles: [`
    .pair-tile {
      display: inline-flex; align-items: center; gap: 8px;
      padding: 6px 12px;
      background: linear-gradient(180deg, #FFFFFF 0%, var(--surface-soft) 100%);
      border: 1px solid var(--border-subtle);
      border-radius: 10px;
      font-family: 'SF Mono', 'Menlo', monospace;
      font-size: 13px;
      font-weight: 700;
    }
    .pair-tile__from { color: var(--brand-navy); }
    .pair-tile__to   { color: var(--brand-cyan); }
    .pair-tile__arrow { font-size: 16px; width: 16px; height: 16px; color: var(--text-subtle); }
  `],
})
export class CurrencyPairListComponent implements OnInit {
  private api = inject(CurrencyPairService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  displayed = ['id', 'pair', 'active', 'actions'];
  rows = signal<CurrencyPair[]>([]);
  total = signal(0);
  page = signal(0);
  size = signal(10);
  loading = signal(false);
  activeCount = computed(() => this.rows().filter(r => r.active).length);

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.list(this.page(), this.size(), 'id,asc').subscribe({
      next: r => { this.rows.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: e => { this.snack.open('Failed to load: ' + e.message, 'OK', { duration: 4000 }); this.loading.set(false); },
    });
  }

  onPage(e: PageEvent) { this.page.set(e.pageIndex); this.size.set(e.pageSize); this.load(); }

  openForm(row: CurrencyPair | null) {
    const ref = this.dialog.open(CurrencyPairFormComponent, { data: { pair: row }, width: '640px' });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      const op = row?.id != null ? this.api.update(row.id, result) : this.api.create(result);
      op.subscribe({
        next: () => { this.snack.open('Saved', 'OK', { duration: 2000 }); this.load(); },
        error: e => this.snack.open('Save failed: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 }),
      });
    });
  }

  remove(row: CurrencyPair) {
    if (!row.id || !confirm(`Delete pair ${row.fromCurrency}/${row.toCurrency}?`)) return;
    this.api.delete(row.id).subscribe({
      next: () => { this.snack.open('Deleted', 'OK', { duration: 2000 }); this.load(); },
      error: e => this.snack.open('Delete failed: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 }),
    });
  }
}
