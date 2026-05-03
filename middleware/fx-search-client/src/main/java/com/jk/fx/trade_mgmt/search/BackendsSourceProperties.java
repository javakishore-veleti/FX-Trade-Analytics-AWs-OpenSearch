package com.jk.fx.trade_mgmt.search;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls which {@link BackendsSource} the search-client wires up.
 *
 * <pre>
 * fx:
 *   opensearch:
 *     source:
 *       type: masterdata           # default: yaml
 *       masterdata-url: http://localhost:8083
 *       ttl-seconds: 60
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "fx.opensearch.source")
public class BackendsSourceProperties {

    public enum Type { yaml, masterdata }

    /** Which source feeds the OpenSearch client factory. */
    private Type type = Type.yaml;

    /** Base URL of fx-masterdata-service when {@link #type} is {@code masterdata}. */
    private String masterdataUrl = "http://localhost:8083";

    /** How long {@link MasterDataBackendsSource} caches results between fetches. */
    private long ttlSeconds = 60;

    /** Per-request timeout for the masterdata HTTP call. */
    private long requestTimeoutMillis = 5_000;
}
