package com.jk.fx.trade_mgmt.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import com.jk.fx.trade_mgmt.service.OpenSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeIndexerConsumer {

    private final OpenSearchService openSearchService;
    private final ObjectMapper mapper;

    @KafkaListener(topics = "trade-events-enriched", groupId = "indexer-group")
    public void consume(String message) throws Exception {
        TradeEventDTO trade = mapper.readValue(message, TradeEventDTO.class);
        openSearchService.indexTrade(trade);
    }
}
