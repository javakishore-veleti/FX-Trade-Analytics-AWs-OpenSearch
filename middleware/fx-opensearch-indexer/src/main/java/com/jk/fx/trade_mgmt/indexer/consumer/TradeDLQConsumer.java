package com.jk.fx.trade_mgmt.indexer.consumer;

import com.jk.fx.trade_mgmt.common.dto.TradeEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TradeDLQConsumer {

    @KafkaListener(topics = "trade-events-dlq", groupId = "dlq-group")
    public void consumeDLQ(TradeEventDto event) {
        log.error("DLQ EVENT RECEIVED: {}", event.getTradeId());
    }
}
