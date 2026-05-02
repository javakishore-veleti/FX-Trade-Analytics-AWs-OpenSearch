package com.jk.fx.trade_mgmt.api;

import com.jk.fx.trade_mgmt.service.TradeSearchService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades/search")
@RequiredArgsConstructor
public class TradeSearchController {

    private final TradeSearchService service;

    /** Legacy endpoint — returns raw OpenSearch response toString(). Prefer GET /trades/search. */
    @GetMapping("/risk")
    public String searchByRisk(@RequestParam String risk) {
        return service.searchByRisk(risk);
    }

    @GetMapping
    public List<Map<String, Object>> search(
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "50") int size) {
        return service.search(risk, region, size);
    }
}
