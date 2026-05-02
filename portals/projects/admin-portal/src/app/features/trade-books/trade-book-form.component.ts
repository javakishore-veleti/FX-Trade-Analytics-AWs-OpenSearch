import { Component, Inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { TradeBook } from '../../shared/models/trade-book.model';

export interface TradeBookDialogData {
  book: TradeBook | null;
}

@Component({
  selector: 'app-trade-book-form',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Edit' : 'New' }} Trade Book</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-row">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" maxlength="40" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" maxlength="120" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Region</mat-label>
          <input matInput formControlName="region" maxlength="40" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Owner</mat-label>
          <input matInput formControlName="owner" maxlength="80" />
        </mat-form-field>
        <mat-form-field appearance="outline" style="flex: 1 1 100%">
          <mat-label>Description</mat-label>
          <textarea matInput rows="3" formControlName="description" maxlength="500"></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="active">Active</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid" (click)="save()">Save</button>
    </mat-dialog-actions>
  `,
})
export class TradeBookFormComponent {
  form;
  isEdit: boolean;

  constructor(
    fb: FormBuilder,
    private ref: MatDialogRef<TradeBookFormComponent, TradeBook>,
    @Inject(MAT_DIALOG_DATA) public data: TradeBookDialogData,
  ) {
    this.isEdit = !!data.book;
    this.form = fb.group({
      code: [data.book?.code ?? '', [Validators.required, Validators.maxLength(40)]],
      name: [data.book?.name ?? '', [Validators.required, Validators.maxLength(120)]],
      region: [data.book?.region ?? ''],
      owner: [data.book?.owner ?? ''],
      description: [data.book?.description ?? ''],
      active: [data.book?.active ?? true],
    });
  }

  save() {
    const v = this.form.getRawValue();
    this.ref.close({
      id: this.data.book?.id,
      code: v.code!,
      name: v.name!,
      region: v.region ?? undefined,
      owner: v.owner ?? undefined,
      description: v.description ?? undefined,
      active: !!v.active,
    });
  }
}
