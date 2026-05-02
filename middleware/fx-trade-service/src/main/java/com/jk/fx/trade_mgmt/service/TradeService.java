package com.jk.fx.trade_mgmt.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.jk.fx.trade_mgmt.kafka.TradeProducer;

@Service
public class TradeService {

    @Autowired
    private TradeProducer producer;

    public void processTrade(String trade) {
        System.out.println("Validate -> Enrich -> Publish");
        producer.send(trade);
    }
}