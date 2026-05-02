
package com.jk.fx.trade_mgmt.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TradeEventDTO {
    private String tradeId;
    private String traderBook;
    private String fromCurrency;
    private String toCurrency;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private BigDecimal rate;
    private BigDecimal discount;
    private BigDecimal totalPrice;
    private BigDecimal totalCost;
    private String region;
    private long timestamp;
}
