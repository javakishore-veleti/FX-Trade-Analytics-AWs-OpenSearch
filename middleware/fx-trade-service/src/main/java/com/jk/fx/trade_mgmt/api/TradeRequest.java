package com.jk.fx.trade_mgmt.api;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TradeRequest {
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal fromAmount;
    private BigDecimal rate;
    private String region;
    private String traderBook;
}
