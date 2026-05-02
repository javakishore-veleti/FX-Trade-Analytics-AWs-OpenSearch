package com.jk.fx.trade_mgmt.common.dto;

import java.math.BigDecimal;

public class TradeReqDto {
    private String tradeId;
    private String traderBook;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal fromQty;
    private BigDecimal toQty;
    private BigDecimal rate;
    private BigDecimal discountPct;

    // Getters and Setters
}
