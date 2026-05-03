package com.jk.fx.trade_mgmt.search;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-wires the OpenSearch client factory in any consuming Spring Boot
 * service. Two services depend on this module today:
 *   - fx-trade-service        (search path)
 *   - fx-opensearch-indexer   (write path)
 *
 * <p>Each service's @SpringBootApplication scans its own package; this class
 * lives in {@code com.jk.fx.trade_mgmt.search} which is under both services'
 * base scan package ({@code com.jk.fx.trade_mgmt}), so component-scan picks
 * it up automatically. No explicit {@code @Import} required.
 */
@AutoConfiguration
@EnableConfigurationProperties(OpenSearchBackendsProperties.class)
public class SearchClientAutoConfiguration {

    @Bean
    public OpenSearchClientFactory openSearchClientFactory(OpenSearchBackendsProperties props) {
        return new DefaultOpenSearchClientFactory(props);
    }
}
