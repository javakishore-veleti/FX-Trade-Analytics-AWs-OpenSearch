import { Component, Inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { Currency } from '../../shared/models/currency.model';

export interface CurrencyDialogData {
  currency: Currency | null;
}

@Component({
  selector: 'app-currency-form',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Edit' : 'New' }} Currency</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-row">
        <mat-form-field appearance="outline">
          <mat-label>ISO Code (3 chars)</mat-label>
          <input matInput formControlName="code" maxlength="3" [readonly]="isEdit" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Country</mat-label>
          <input matInput formControlName="country" />
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
export class CurrencyFormComponent {
  form;
  isEdit: boolean;

  constructor(
    fb: FormBuilder,
    private ref: MatDialogRef<CurrencyFormComponent, Currency>,
    @Inject(MAT_DIALOG_DATA) public data: CurrencyDialogData,
  ) {
    this.isEdit = !!data.currency;
    this.form = fb.group({
      code: [{ value: data.currency?.code ?? '', disabled: this.isEdit }, [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
      name: [data.currency?.name ?? '', [Validators.required, Validators.maxLength(80)]],
      country: [data.currency?.country ?? ''],
      active: [data.currency?.active ?? true],
    });
  }

  save() {
    const v = this.form.getRawValue();
    this.ref.close({
      code: (v.code ?? '').toUpperCase(),
      name: v.name ?? '',
      country: v.country ?? undefined,
      active: !!v.active,
    });
  }
}
