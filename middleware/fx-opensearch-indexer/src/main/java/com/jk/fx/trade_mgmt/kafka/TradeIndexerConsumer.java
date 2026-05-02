
package com.jk.fx.trade_mgmt.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TradeIndexerConsumer {
  @KafkaListener(topics="trade-events", groupId="indexer-group")
  public void consume(String msg){
    System.out.println("INDEX: "+msg);
  }
}
