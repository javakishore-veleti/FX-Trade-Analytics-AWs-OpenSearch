package com.jk.fx.trade_mgmt.search;

import org.opensearch.client.opensearch.OpenSearchClient;

/**
 * Per-region OpenSearch client lookup. Implementations cache singleton clients
 * keyed by region; transport (Apache HTTP vs SigV4) is chosen at build time
 * from the backend's {@link OpenSearchBackend#getProvider() provider}.
 *
 * <p>Application code never news up an {@code OpenSearchClient} directly;
 * always go through the factory.
 */
public interface OpenSearchClientFactory {

    /**
     * @param region the region key declared under {@code fx.opensearch.backends}
     * @return a thread-safe, reusable OpenSearchClient for that region
     * @throws IllegalArgumentException if the region is not configured
     */
    OpenSearchClient clientFor(String region);

    /** @return true if the factory has a backend configured for {@code region}. */
    boolean hasRegion(String region);
}
