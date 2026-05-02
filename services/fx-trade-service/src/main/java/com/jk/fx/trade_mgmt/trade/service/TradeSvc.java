package com.jk.fx.trade_mgmt.trade.service;

import com.jk.fx.trade_mgmt.common.dto.TradeReqDto;
import com.jk.fx.trade_mgmt.common.dto.TradeRespDto;

public interface TradeSvc {
    TradeRespDto createTrade(TradeReqDto req);
}
