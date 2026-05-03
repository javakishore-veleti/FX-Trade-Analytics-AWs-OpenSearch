package com.jk.fx.trade_mgmt.service;

import com.jk.fx.trade_mgmt.search.OpenSearchClientFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeSearchService {

    private final OpenSearchClientFactory clientFactory;

    /**
     * Legacy: kept for backward compatibility with the existing UI / smoke
     * tests. Targets the {@link #defaultRegion()} backend.
     */
    public String searchByRisk(String risk) {
        List<Map<String, Object>> hits = search(risk, defaultRegion(), 50);
        return "Found " + hits.size() + " trade(s) with riskLevel=" + risk;
    }

    /**
     * Single-region search. {@code region} drives BOTH which OpenSearch
     * domain to hit AND which index to query ({@code fx-trades-{region}}).
     *
     * <p>Cross-region "All regions" search is intentionally NOT supported here
     * — that's the AWS OpenSearch UI's federation responsibility (see the
     * design doc {@code docs/design/AWS-OpenSearch-Cross-Region-Use-Cases.md}).
     */
    public List<Map<String, Object>> search(String risk, String region, int size) {
        if (region == null || region.isBlank()) {
            region = defaultRegion();
        }
        final String chosenRegion = region;
        final String indexName = "fx-trades-" + chosenRegion;
        final int safeSize = Math.min(Math.max(size, 1), 200);

        try {
            OpenSearchClient client = clientFactory.clientFor(chosenRegion);

            // Compose query
            List<Query> mustClauses = new ArrayList<>();
            if (risk != null && !risk.isBlank()) {
                final String riskUp = risk.toUpperCase();
                mustClauses.add(Query.of(q -> q.term(t -> t
                        .field("riskLevel")
                        .value(FieldValue.of(riskUp)))));
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
                out.add(source);
            }
            log.debug("Search region={} risk={} → {} hits", chosenRegion, risk, out.size());
            return out;
        } catch (Exception e) {
            throw new RuntimeException(
                    "OpenSearch search failed for region " + chosenRegion + " (index=" + indexName + ")", e);
        }
    }

    /** Sane fallback so existing single-region callers still work without supplying region. */
    private String defaultRegion() {
        return "us-east-1";
    }
}
