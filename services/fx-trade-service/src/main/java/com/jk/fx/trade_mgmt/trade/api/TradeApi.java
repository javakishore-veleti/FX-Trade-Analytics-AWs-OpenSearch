package com.jk.fx.trade_mgmt.trade.api;

import com.jk.fx.trade_mgmt.common.dto.TradeReqDto;
import com.jk.fx.trade_mgmt.common.dto.TradeRespDto;
import com.jk.fx.trade_mgmt.trade.service.TradeSvc;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
public class TradeApi {

    private final TradeSvc tradeSvc;

    public TradeApi(TradeSvc tradeSvc) {
        this.tradeSvc = tradeSvc;
    }

    @PostMapping
    public TradeRespDto createTrade(@RequestBody TradeReqDto req) {
        return tradeSvc.createTrade(req);
    }
}
