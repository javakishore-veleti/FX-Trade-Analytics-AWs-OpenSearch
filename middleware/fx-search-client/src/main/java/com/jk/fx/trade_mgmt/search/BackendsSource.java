package com.jk.fx.trade_mgmt.search;

import java.util.Collection;

/**
 * Strategy that resolves the current set of OpenSearch backends. Two
 * implementations ship in this module:
 *
 * <ul>
 *   <li>{@link YamlBackendsSource} — reads {@code fx.opensearch.backends}
 *       from {@code application.yml}. Static for the lifetime of the JVM.</li>
 *   <li>{@link MasterDataBackendsSource} — calls the master-data service's
 *       {@code GET /api/admin/opensearch-deployments} endpoint, filters to
 *       ACTIVE rows that carry a usable endpoint, and converts them to
 *       {@link OpenSearchBackend}s. Memoised with a TTL so a fresh sync in
 *       the admin portal becomes effective within
 *       {@code fx.opensearch.source.ttl-seconds} (default 60s).</li>
 * </ul>
 *
 * <p>The choice is driven by {@code fx.opensearch.source.type=yaml|masterdata}
 * in {@code application.yml} (default {@code yaml}).
 */
public interface BackendsSource {

    /**
     * Returns the current best-known backend set. Implementations are free to
     * cache; {@link DefaultOpenSearchClientFactory} treats results as
     * authoritative for the lifetime of the call.
     *
     * <p>Must not throw on transient backend errors — return the last known
     * good snapshot instead, or an empty collection if there has never been
     * one. Logging of failures is the implementation's responsibility.
     */
    Collection<OpenSearchBackend> load();
}
