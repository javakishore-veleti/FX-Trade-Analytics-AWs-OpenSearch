package com.jk.fx.trade_mgmt.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.jk.fx.trade_mgmt.service.TradeService;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    @Autowired
    private TradeService service;

    @PostMapping
    public String createTrade() {
        service.processTrade("sample-trade");
        return "Trade sent";
    }
}