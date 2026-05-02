package com.jk.fx.trade_mgmt.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DLQConsumer {

    @KafkaListener(topics = "trade-events-dlq", groupId = "dlq-group")
    public void consume(String message) {

        System.out.println("💀 DLQ MESSAGE: " + message);

        // Later:
        // send alert / store in DB / trigger reprocess
    }
}