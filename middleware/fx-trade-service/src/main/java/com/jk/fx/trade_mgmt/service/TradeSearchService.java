package com.jk.fx.trade_mgmt.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeSearchService {

    private final RestHighLevelClient client;

    /** Legacy: kept for backward compatibility with existing UI/tests. */
    public String searchByRisk(String risk) {
        try {
            SearchRequest request = new SearchRequest("fx-trades-*");
            SearchSourceBuilder builder = new SearchSourceBuilder()
                    .query(QueryBuilders.matchQuery("riskLevel", risk));
            request.source(builder);
            return client.search(request, RequestOptions.DEFAULT).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> search(String risk, String region, int size) {
        try {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            if (risk != null && !risk.isBlank()) {
                bool.must(QueryBuilders.termQuery("riskLevel", risk.toUpperCase()));
            }
            if (region != null && !region.isBlank()) {
                bool.must(QueryBuilders.termQuery("region", region));
            }
            if (!bool.hasClauses()) {
                bool.must(QueryBuilders.matchAllQuery());
            }

            SearchSourceBuilder src = new SearchSourceBuilder()
                    .query(bool)
                    .size(Math.min(Math.max(size, 1), 200))
                    .sort("timestamp", SortOrder.DESC);

            SearchRequest req = new SearchRequest("fx-trades-*").source(src);
            SearchResponse resp = client.search(req, RequestOptions.DEFAULT);

            List<Map<String, Object>> hits = new ArrayList<>();
            for (SearchHit h : resp.getHits().getHits()) {
                hits.add(h.getSourceAsMap());
            }
            return hits;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
