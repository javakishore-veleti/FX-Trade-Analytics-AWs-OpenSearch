export interface TradeBook {
  id?: number;
  code: string;
  name: string;
  description?: string;
  region?: string;
  owner?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}
