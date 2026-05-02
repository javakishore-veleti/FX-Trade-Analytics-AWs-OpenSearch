package com.jk.fx.trade_mgmt.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.jk.fx.trade_mgmt.service.TradeService;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService service;

    @PostMapping
    public String createTrade() {
        service.processTrade("sample-trade");
        return "Trade sent";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}