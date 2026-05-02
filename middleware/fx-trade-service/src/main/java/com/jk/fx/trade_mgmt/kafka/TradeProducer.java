package com.jk.fx.trade_mgmt.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class TradeProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    public void send(String msg) {
        kafkaTemplate.send("trade-events", msg);
    }
}