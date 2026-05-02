package com.jk.fx.trade_mgmt.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TradePlaceResponse {
    private String tradeId;
    private boolean accepted;
    private String reason;
}
