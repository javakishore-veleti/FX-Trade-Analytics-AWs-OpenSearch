package com.jk.fx.trade_mgmt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.jk.fx.trade_mgmt.kafka.TradeProducer;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeProducer producer;

    public void processTrade(String trade) {
        System.out.println("Validate -> Enrich -> Publish");
        producer.send(trade);
    }
}