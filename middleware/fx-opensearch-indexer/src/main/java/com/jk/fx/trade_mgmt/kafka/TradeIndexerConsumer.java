package com.jk.fx.trade_mgmt.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import com.jk.fx.trade_mgmt.service.OpenSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeIndexerConsumer {

    private final OpenSearchService openSearchService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "trade-events", groupId = "indexer-group")
    public void consume(String message) {
        try {
            TradeEventDTO trade =
                    mapper.readValue(message, TradeEventDTO.class);

            openSearchService.indexTrade(trade);

        } catch (Exception e) {
            System.out.println("❌ Failed to process message: " + message);
        }
    }
}