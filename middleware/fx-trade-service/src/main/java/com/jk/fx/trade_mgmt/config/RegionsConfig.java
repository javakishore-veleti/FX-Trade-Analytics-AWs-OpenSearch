package com.jk.fx.trade_mgmt.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Maps each region code to the trade-service URL that owns trades in that region.
 * Bound from <code>fx.regions.*</code> in application.yml. Overridable per environment
 * via env vars, e.g. <code>FX_REGIONS_ENDPOINTS_US_EAST_1</code>.
 */
@Configuration
@ConfigurationProperties(prefix = "fx.regions")
@Data
public class RegionsConfig {
    /** region code → backend URL (with scheme + host + port, no trailing slash). */
    private Map<String, String> endpoints = new LinkedHashMap<>();
}
