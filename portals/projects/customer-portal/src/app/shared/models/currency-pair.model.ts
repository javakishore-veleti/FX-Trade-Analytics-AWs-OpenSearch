export interface CurrencyPair {
  id?: number;
  fromCurrency: string;
  toCurrency: string;
  active: boolean;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
