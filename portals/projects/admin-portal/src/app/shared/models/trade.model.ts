export interface Trade {
  _id?: string;
  tradeId?: string;
  traderBook?: string;
  fromCurrency?: string;
  toCurrency?: string;
  fromAmount?: number;
  toAmount?: number;
  rate?: number;
  region?: string;
  riskLevel?: string;        // 'LOW' | 'MEDIUM' | 'HIGH'
  timestamp?: string | number;
  // Tolerant — anything else the source doc carried passes through
  [key: string]: unknown;
}
