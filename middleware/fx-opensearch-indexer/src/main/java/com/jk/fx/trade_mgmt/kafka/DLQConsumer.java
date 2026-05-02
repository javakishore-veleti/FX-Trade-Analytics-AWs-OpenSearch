package com.jk.fx.trade_mgmt.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DLQConsumer {

    @KafkaListener(topics = "trade-index-dlq", groupId = "indexer-dlq")
    public void consume(String msg) {
        System.out.println("💀 INDEX DLQ: " + msg);
    }
}