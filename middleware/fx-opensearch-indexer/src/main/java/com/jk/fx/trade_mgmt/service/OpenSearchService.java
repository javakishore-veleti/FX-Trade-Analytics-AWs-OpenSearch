package com.jk.fx.trade_mgmt.service;

import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import com.jk.fx.trade_mgmt.search.OpenSearchClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenSearchService {

    private final OpenSearchClientFactory clientFactory;

    /**
     * Routes the write to the OpenSearch domain that matches {@code trade.region}.
     * Index name follows the {@code fx-trades-{region}} convention so the AWS
     * OpenSearch UI can federate via the {@code fx-trades-*} pattern.
     */
    public void indexTrade(TradeEventDTO trade) {
        if (trade.getRegion() == null || trade.getRegion().isBlank()) {
            throw new IllegalArgumentException("Trade.region is required (tradeId=" + trade.getTradeId() + ")");
        }
        String region = trade.getRegion();
        String indexName = "fx-trades-" + region;

        try {
            OpenSearchClient client = clientFactory.clientFor(region);
            IndexResponse resp = client.index(req -> req
                    .index(indexName)
                    .id(trade.getTradeId())
                    .document(trade));
            log.info("✅ Indexed trade {} → {} (result={})",
                    trade.getTradeId(), indexName, resp.result());
        } catch (Exception e) {
            throw new RuntimeException(
                    "OpenSearch indexing failed for trade " + trade.getTradeId() + " in region " + region, e);
        }
    }
}
