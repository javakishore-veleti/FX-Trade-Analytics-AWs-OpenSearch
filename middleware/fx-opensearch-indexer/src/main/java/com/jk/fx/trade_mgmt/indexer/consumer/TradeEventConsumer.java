package com.jk.fx.trade_mgmt.indexer.consumer;

import com.jk.fx.trade_mgmt.common.dto.TradeEventDto;
import com.jk.fx.trade_mgmt.indexer.service.OpenSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeEventConsumer {

    private final OpenSearchService openSearchService;

    @KafkaListener(topics = "trade-events", groupId = "indexer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consume(TradeEventDto event) {

        log.info("Processing trade: {}", event.getTradeId());

        openSearchService.indexTrade(event);
    }
}
