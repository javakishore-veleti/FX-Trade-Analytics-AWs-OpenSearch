
package com.jk.fx.trade_mgmt.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TradeRiskConsumer {
  @KafkaListener(topics="trade-events", groupId="risk-group")
  public void consume(String msg){
    System.out.println("RISK: "+msg);
  }
}
