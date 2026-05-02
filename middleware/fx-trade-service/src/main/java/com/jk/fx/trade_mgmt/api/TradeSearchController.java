package com.jk.fx.trade_mgmt.controller;

import com.jk.fx.trade_mgmt.service.TradeSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trades/search")
@RequiredArgsConstructor
public class TradeSearchController {

    private final TradeSearchService service;

    @GetMapping("/risk")
    public String searchByRisk(@RequestParam String risk) {
        return service.searchByRisk(risk);
    }
}