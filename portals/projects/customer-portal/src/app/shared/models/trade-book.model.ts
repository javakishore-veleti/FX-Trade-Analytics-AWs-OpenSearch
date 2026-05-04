export interface TradeBook {
  id: number;
  code: string;
  name: string;
  description?: string;
  region?: string;
  owner?: string;
  active: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
