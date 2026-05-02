import { Component, OnInit, signal, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TradeBookService } from '../../shared/services/trade-book.service';
import { TradeBook } from '../../shared/models/trade-book.model';
import { TradeBookFormComponent } from './trade-book-form.component';

@Component({
  selector: 'app-trade-book-list',
  standalone: true,
  imports: [
    DatePipe, MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatDialogModule, MatSnackBarModule, MatProgressBarModule,
  ],
  template: `
    <div class="actions" style="margin-bottom: 12px">
      <h2 style="margin: 0">Trade Books</h2>
      <span class="spacer" style="flex: 1"></span>
      <button mat-flat-button color="primary" (click)="openForm(null)">
        <mat-icon>add</mat-icon> New Trade Book
      </button>
    </div>

    @if (loading()) { <mat-progress-bar mode="indeterminate"></mat-progress-bar> }

    <table mat-table [dataSource]="rows()">
      <ng-container matColumnDef="code">
        <th mat-header-cell *matHeaderCellDef>Code</th>
        <td mat-cell *matCellDef="let r">{{ r.code }}</td>
      </ng-container>
      <ng-container matColumnDef="name">
        <th mat-header-cell *matHeaderCellDef>Name</th>
        <td mat-cell *matCellDef="let r">{{ r.name }}</td>
      </ng-container>
      <ng-container matColumnDef="region">
        <th mat-header-cell *matHeaderCellDef>Region</th>
        <td mat-cell *matCellDef="let r">{{ r.region }}</td>
      </ng-container>
      <ng-container matColumnDef="owner">
        <th mat-header-cell *matHeaderCellDef>Owner</th>
        <td mat-cell *matCellDef="let r">{{ r.owner }}</td>
      </ng-container>
      <ng-container matColumnDef="active">
        <th mat-header-cell *matHeaderCellDef>Active</th>
        <td mat-cell *matCellDef="let r">{{ r.active ? '✓' : '—' }}</td>
      </ng-container>
      <ng-container matColumnDef="updated">
        <th mat-header-cell *matHeaderCellDef>Updated</th>
        <td mat-cell *matCellDef="let r">{{ r.updatedAt | date:'short' }}</td>
      </ng-container>
      <ng-container matColumnDef="actions">
        <th mat-header-cell *matHeaderCellDef></th>
        <td mat-cell *matCellDef="let r">
          <button mat-icon-button (click)="openForm(r)"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button color="warn" (click)="remove(r)"><mat-icon>delete</mat-icon></button>
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="displayed"></tr>
      <tr mat-row *matRowDef="let row; columns: displayed"></tr>
    </table>

    <mat-paginator
      [length]="total()"
      [pageSize]="size()"
      [pageIndex]="page()"
      [pageSizeOptions]="[10, 20, 50]"
      (page)="onPage($event)">
    </mat-paginator>
  `,
})
export class TradeBookListComponent implements OnInit {
  private api = inject(TradeBookService);
  private dialog = inject(MatDialog);
  private snack = inject(MatSnackBar);

  displayed = ['code', 'name', 'region', 'owner', 'active', 'updated', 'actions'];
  rows = signal<TradeBook[]>([]);
  total = signal(0);
  page = signal(0);
  size = signal(10);
  loading = signal(false);

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.api.list(this.page(), this.size(), 'code,asc').subscribe({
      next: r => { this.rows.set(r.content); this.total.set(r.totalElements); this.loading.set(false); },
      error: e => { this.snack.open('Failed to load: ' + e.message, 'OK', { duration: 4000 }); this.loading.set(false); },
    });
  }

  onPage(e: PageEvent) { this.page.set(e.pageIndex); this.size.set(e.pageSize); this.load(); }

  openForm(row: TradeBook | null) {
    const ref = this.dialog.open(TradeBookFormComponent, { data: { book: row }, width: '720px' });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      const op = row?.id != null ? this.api.update(row.id, result) : this.api.create(result);
      op.subscribe({
        next: () => { this.snack.open('Saved', 'OK', { duration: 2000 }); this.load(); },
        error: e => this.snack.open('Save failed: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 }),
      });
    });
  }

  remove(row: TradeBook) {
    if (!row.id || !confirm(`Delete trade book ${row.code}?`)) return;
    this.api.delete(row.id).subscribe({
      next: () => { this.snack.open('Deleted', 'OK', { duration: 2000 }); this.load(); },
      error: e => this.snack.open('Delete failed: ' + (e.error?.message ?? e.message), 'OK', { duration: 5000 }),
    });
  }
}
