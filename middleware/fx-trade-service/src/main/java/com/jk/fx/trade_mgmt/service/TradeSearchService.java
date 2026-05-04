package com.jk.fx.trade_mgmt.service;

import com.jk.fx.trade_mgmt.search.OpenSearchBackend;
import com.jk.fx.trade_mgmt.search.OpenSearchBackendsProperties;
import com.jk.fx.trade_mgmt.search.OpenSearchClientFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

/**
 * App-side trade search. Two modes:
 *   <ul>
 *     <li><b>Single region</b>: {@link #search(String, String, int)} hits one
 *         OpenSearch domain. Original behaviour, used by single-region
 *         callers.</li>
 *     <li><b>Multi-region fan-out</b>: {@link #searchMulti(String, List, int)}
 *         queries N domains in sequence and merges results. Used by the
 *         "cross-region" mode in admin + customer portals. An empty / null
 *         {@code regions} list expands to every backend in
 *         {@code fx.opensearch.backends}.</li>
 *   </ul>
 *
 * <p>Multi-region fan-out is app-side federation — independent of (and
 * complementary to) the AWS OpenSearch cross-region UI feature. It works
 * today against any combination of regional domains the search-client
 * factory knows about.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TradeSearchService {

    private final OpenSearchClientFactory clientFactory;
    private final OpenSearchBackendsProperties backendsProperties;

    /**
     * Legacy: kept for backward compatibility with the existing UI / smoke
     * tests. Targets the {@link #defaultRegion()} backend.
     */
    public String searchByRisk(String risk) {
        List<Map<String, Object>> hits = search(risk, defaultRegion(), null, 50);
        return "Found " + hits.size() + " trade(s) with riskLevel=" + risk;
    }

    /**
     * Single-region search. {@code region} drives BOTH which OpenSearch
     * domain to hit AND which index to query ({@code fx-trades-{region}}).
     * Optional {@code traderBook} narrows to one trading book.
     */
    public List<Map<String, Object>> search(String risk, String region, String traderBook, int size) {
        if (region == null || region.isBlank()) {
            region = defaultRegion();
        }
        return searchOneRegion(risk, traderBook, region, size);
    }

    /** Backward-compatible 3-arg overload for callers that don't filter by trader book. */
    public List<Map<String, Object>> search(String risk, String region, int size) {
        return search(risk, region, null, size);
    }

    /** Backward-compatible 3-arg overload that doesn't filter by trader book. */
    public List<Map<String, Object>> searchMulti(String risk, List<String> regions, int size) {
        return searchMulti(risk, regions, null, size);
    }

    /**
     * Multi-region fan-out. Searches every region in {@code regions} (or every
     * configured backend if the list is null/empty), merges the hits, sorts
     * by {@code timestamp} desc across the merged set, truncates to
     * {@code size}. Each hit's {@code region} field is preserved so the UI
     * can display "from which region it came from" per row. Optional
     * {@code traderBook} narrows results to one trading book.
     */
    public List<Map<String, Object>> searchMulti(String risk, List<String> regions, String traderBook, int size) {
        List<String> targetRegions = (regions == null || regions.isEmpty())
                ? configuredRegions()
                : regions.stream()
                        .filter(r -> r != null && !r.isBlank())
                        .map(String::trim)
                        .distinct()
                        .collect(Collectors.toList());

        if (targetRegions.isEmpty()) {
            log.warn("searchMulti: no target regions resolved.");
            return List.of();
        }

        final int perRegionSize = Math.min(Math.max(size, 1), 200);
        List<Map<String, Object>> merged = new ArrayList<>();
        List<String> errored = new ArrayList<>();

        for (String region : targetRegions) {
            try {
                List<Map<String, Object>> hits = searchOneRegion(risk, traderBook, region, perRegionSize);
                // Defensive: ensure each hit carries its region for UI display
                // even on indices where the field somehow isn't present.
                for (Map<String, Object> h : hits) {
                    h.putIfAbsent("region", region);
                }
                merged.addAll(hits);
            } catch (Exception ex) {
                log.warn("searchMulti: region {} failed ({}); continuing", region, ex.getMessage());
                errored.add(region);
            }
        }

        // Sort merged set by timestamp desc, then truncate.
        merged.sort(Comparator.comparing(
                (Map<String, Object> m) -> {
                    Object t = m.get("timestamp");
                    if (t instanceof Number n) return n.longValue();
                    if (t instanceof String s) {
                        try { return Long.parseLong(s); }
                        catch (NumberFormatException e) { return 0L; }
                    }
                    return 0L;
                }).reversed());

        if (merged.size() > size) {
            merged = merged.subList(0, size);
        }
        log.info("searchMulti: regions={} merged={} hits (errors in {})",
                targetRegions, merged.size(), errored);
        return merged;
    }

    private List<Map<String, Object>> searchOneRegion(String risk, String traderBook, String region, int size) {
        final String indexName = "fx-trades-" + region;
        final int safeSize = Math.min(Math.max(size, 1), 200);

        try {
            OpenSearchClient client = clientFactory.clientFor(region);

            List<Query> mustClauses = new ArrayList<>();
            if (risk != null && !risk.isBlank()) {
                final String riskUp = risk.toUpperCase();
                mustClauses.add(Query.of(q -> q.term(t -> t
                        .field("riskLevel")
                        .value(FieldValue.of(riskUp)))));
            }
            if (traderBook != null && !traderBook.isBlank()) {
                final String bookValue = traderBook.trim();
                mustClauses.add(Query.of(q -> q.term(t -> t
                        .field("traderBook")
                        .value(FieldValue.of(bookValue)))));
            }

            SearchResponse<Map> resp = client.search(s -> s
                            .index(indexName)
                            .size(safeSize)
                            .sort(so -> so.field(f -> f.field("timestamp").order(SortOrder.Desc)))
                            .query(q -> q.bool(b -> {
                                if (mustClauses.isEmpty()) {
                                    b.must(Query.of(qq -> qq.matchAll(m -> m)));
                                } else {
                                    b.must(mustClauses);
                                }
                                return b;
                            })),
                    Map.class);

            List<Map<String, Object>> out = new ArrayList<>(resp.hits().hits().size());
            for (Hit<Map> h : resp.hits().hits()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> source = h.source() != null ? new HashMap<>(h.source()) : new HashMap<>();
                source.put("_id", h.id());
                source.putIfAbsent("region", region);
                out.add(source);
            }
            log.debug("Search region={} risk={} → {} hits", region, risk, out.size());
            return out;
        } catch (Exception e) {
            // Index doesn't exist yet (zero trades in this region — common on a fresh
            // local OpenSearch before any trade has been placed). Return empty instead
            // of 500 so the Recent Trades / Search Trades pages render an empty state.
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("index_not_found_exception") || msg.contains("no such index")) {
                log.debug("Index {} doesn't exist yet for region {}; returning empty result set.",
                        indexName, region);
                return List.of();
            }
            throw new RuntimeException(
                    "OpenSearch search failed for region " + region + " (index=" + indexName + ")", e);
        }
    }

    /** All region keys currently configured in {@code fx.opensearch.backends}. */
    private List<String> configuredRegions() {
        List<OpenSearchBackend> backends = backendsProperties.getBackends();
        if (backends == null) return List.of();
        return backends.stream()
                .map(OpenSearchBackend::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /** Sane fallback so existing single-region callers still work without supplying region. */
    private String defaultRegion() {
        return "us-east-1";
    }
}
