import { Component, Inject, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { CurrencyPair } from '../../shared/models/currency-pair.model';
import { Currency } from '../../shared/models/currency.model';
import { CurrencyService } from '../../shared/services/currency.service';

export interface CurrencyPairDialogData {
  pair: CurrencyPair | null;
}

@Component({
  selector: 'app-currency-pair-form',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatSelectModule,
    MatCheckboxModule, MatButtonModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Edit' : 'New' }} Currency Pair</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-row">
        <mat-form-field appearance="outline">
          <mat-label>From</mat-label>
          <mat-select formControlName="fromCurrency">
            @for (c of currencies; track c.code) {
              <mat-option [value]="c.code">{{ c.code }} — {{ c.name }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>To</mat-label>
          <mat-select formControlName="toCurrency">
            @for (c of currencies; track c.code) {
              <mat-option [value]="c.code">{{ c.code }} — {{ c.name }}</mat-option>
            }
          </mat-select>
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
export class CurrencyPairFormComponent implements OnInit {
  private currencyApi = inject(CurrencyService);
  form;
  isEdit: boolean;
  currencies: Currency[] = [];

  constructor(
    fb: FormBuilder,
    private ref: MatDialogRef<CurrencyPairFormComponent, CurrencyPair>,
    @Inject(MAT_DIALOG_DATA) public data: CurrencyPairDialogData,
  ) {
    this.isEdit = !!data.pair;
    this.form = fb.group({
      fromCurrency: [data.pair?.fromCurrency ?? '', Validators.required],
      toCurrency: [data.pair?.toCurrency ?? '', Validators.required],
      active: [data.pair?.active ?? true],
    });
  }

  ngOnInit() {
    this.currencyApi.list(0, 200, 'code,asc').subscribe(r => this.currencies = r.content);
  }

  save() {
    const v = this.form.getRawValue();
    this.ref.close({
      id: this.data.pair?.id,
      fromCurrency: v.fromCurrency!,
      toCurrency: v.toCurrency!,
      active: !!v.active,
    });
  }
}
