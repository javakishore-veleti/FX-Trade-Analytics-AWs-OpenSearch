package com.jk.fx.trade_mgmt.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One configured OpenSearch endpoint, keyed by region.
 *
 * <p>Bound from {@code fx.opensearch.backends[*]} in {@code application.yml}:
 *
 * <pre>
 * fx.opensearch.backends:
 *   - region:   us-east-1
 *     provider: aws
 *     endpoint: https://fxs-dev-us-east-1-XXXXXX.us-east-1.es.amazonaws.com
 *   - region:   eu-west-2
 *     provider: aws
 *     endpoint: https://fxs-dev-eu-west-2-YYYYYY.eu-west-2.es.amazonaws.com
 *   - region:   local-dev
 *     provider: local
 *     endpoint: http://localhost:9200
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenSearchBackend {

    public enum Provider { local, aws }

    /** Logical region key. Application code looks up by this string. */
    private String region;

    /** Drives transport selection in the factory. */
    private Provider provider;

    /** Full endpoint URL — http[s]://host[:port], NO trailing slash. */
    private String endpoint;
}
