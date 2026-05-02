export interface TradeRequest {
  fromCurrency: string;
  toCurrency: string;
  fromAmount: number;
  rate: number;
  region?: string;
  traderBook?: string;
}

export interface TradePlaceResponse {
  tradeId: string;
  accepted: boolean;
  reason: string;
}

export interface TradeHit {
  tradeId?: string;
  traderBook?: string;
  fromCurrency?: string;
  toCurrency?: string;
  fromAmount?: number;
  toAmount?: number;
  rate?: number;
  region?: string;
  timestamp?: number;
  riskLevel?: string;
  [key: string]: unknown;
}
