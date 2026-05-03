import { Component, OnInit, signal, inject, computed } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CurrencyService } from '../../shared/services/currency.service';
import { Currency } from '../../shared/models/currency.model';
import { CurrencyFormComponent } from './currency-form.component';

@Component({
  selector: 'app-currency-list',
  standalone: true,
  imports: [
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule, MatTooltipModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule,
  ],
  template: `
    <section class="stat-row">
      <div class="stat-tile">
        <div class="stat-tile__label">Total currencies</div>
        <div class="stat-tile__value">{{ total() }}</div>
        <div class="stat-tile__hint">across all regions</div>
      </div>
      <div class="stat-tile">
        <div class="stat-tile__label">Active</div>
        <div class="stat-tile__value">{{ activeCount() }}</div>
        <div class="stat-tile__hint">enabled for new pairs</div>
      </div>
      <div class="stat-tile">
        <div class="stat-tile__label">Inactive</div>
        <div class="stat-tile__value">{{ total() - activeCount() }}</div>
        <div class="stat-tile__hint">excluded from trading</div>
      </div>
    </section>

    <section class="page-card">
      <header class="page-card__header">
        <div>
          <h2 class="page-card__title">Currencies</h2>
          <p class="page-card__subtitle">Master list of ISO currency codes available for trading</p>
        </div>
        <span class="spacer" style="flex: 1"></span>
        <button mat-flat-button color="primary" (click)="openForm(null)">
          <mat-icon>add</mat-icon> New Currency
        </button>
      </header>

      @if (loading()) { <mat-progress-bar mode="indeterminate"></mat-progress-bar> }

      <div class="page-card__body">
        <table mat-table [dataSource]="rows()">
          <ng-container matColumnDef="code">
            <th mat-header-cell *matHeaderCellDef>Code</th>
            <td mat-cell *matCellDef="let r"><span class="code-badge">{{ r.code }}</span></td>
          </ng-container>
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Name</th>
            <td mat-cell *matCellDef="let r" style="font-weight: 500">{{ r.name }}</td>
          </ng-container>
          <ng-container matColumnDef="country">
            <th mat-header-cell *matHeaderCellDef>Country</th>
            <td mat-cell *matCellDef="let r">{{ r.country }}</td>
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
    .code-badge {
      display: inline-block;
      font-family: 'SF Mono', 'Menlo', monospace;
      font-weight: 700;
      font-size: 13px;
      color: var(--brand-navy);
      background: var(--mat-sys-primary-container);
      padding: 4px 10px;
      border-radius: 6px;
      letter-spacing: 0.04em;
    }
  `],
})
export class CurrencyListComponent implements OnInit {
  private api = inject(CurrencyService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  displayed = ['code', 'name', 'country', 'active', 'actions'];
  rows = signal<Currency[]>([]);
  total = signal(0);
  page = signal(0);
  size = signal(10);
  loading = signal(false);
  activeCount = computed(() => this.rows().filter(r => r.active).length);

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.list(this.page(), this.size(), 'code,asc').subscribe({
      next: r => { this.rows.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: e => { this.snack.open('Failed to load: ' + e.message, 'OK', { duration: 4000 }); this.loading.set(false); },
    });
  }

  onPage(e: PageEvent) { this.page.set(e.pageIndex); this.size.set(e.pageSize); this.load(); }

  openForm(row: Currency | null) {
    const ref = this.dialog.open(CurrencyFormComponent, { data: { currency: row }, width: '640px' });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      const op = row ? this.api.update(row.code, result) : this.api.create(result);
      op.subscribe({
        next: () => { this.snack.open('Saved', 'OK', { duration: 2000 }); this.load(); },
        error: e => this.snack.open('Save failed: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 }),
      });
    });
  }

  remove(row: Currency) {
    if (!confirm(`Delete currency ${row.code}?`)) return;
    this.api.delete(row.code).subscribe({
      next: () => { this.snack.open('Deleted', 'OK', { duration: 2000 }); this.load(); },
      error: e => this.snack.open('Delete failed: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 }),
    });
  }
}
