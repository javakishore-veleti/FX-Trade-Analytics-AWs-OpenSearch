package com.jk.fx.trade_mgmt.api;

import com.jk.fx.trade_mgmt.config.RegionsConfig;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only config surface for the customer portal. Lets the frontend discover
 * which regions exist and where to POST trades for each, without baking the map
 * into Angular environment files.
 */
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final RegionsConfig regions;

    @GetMapping("/regions")
    public Map<String, String> regions() {
        return regions.getEndpoints();
    }
}
