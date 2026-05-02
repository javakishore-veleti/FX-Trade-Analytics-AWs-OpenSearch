package com.jk.fx.trade_mgmt.service;

import lombok.RequiredArgsConstructor;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeSearchService {

    private final RestHighLevelClient client;

    public String searchByRisk(String risk) {
        try {
            SearchRequest request = new SearchRequest("fx-trades-*");

            SearchSourceBuilder builder = new SearchSourceBuilder();
            builder.query(QueryBuilders.matchQuery("riskLevel", risk));

            request.source(builder);

            SearchResponse response =
                    client.search(request, RequestOptions.DEFAULT);

            return response.toString();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}