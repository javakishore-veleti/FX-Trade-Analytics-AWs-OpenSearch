package com.jk.fx.trade_mgmt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import lombok.RequiredArgsConstructor;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.RequestOptions;
import org.opensearch.action.index.IndexRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenSearchService {

    private final RestHighLevelClient client;
    private final ObjectMapper mapper;

    public void indexTrade(TradeEventDTO trade) {
        try {
            String indexName = "fx-trades-" + trade.getRegion();

            IndexRequest request = new IndexRequest(indexName)
                    .id(trade.getTradeId())
                    .source(mapper.convertValue(trade, Map.class));

            client.index(request, RequestOptions.DEFAULT);

            System.out.println("✅ Indexed trade: " + trade.getTradeId());

        } catch (Exception e) {
            throw new RuntimeException("OpenSearch indexing failed", e);
        }
    }
}