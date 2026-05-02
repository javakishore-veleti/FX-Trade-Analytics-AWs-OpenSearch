
package com.jk.fx.trade_mgmt.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;

@Service
@RequiredArgsConstructor
public class TradeProducer {

  private final KafkaTemplate<String,String> kafkaTemplate;
  private final ObjectMapper mapper = new ObjectMapper();

  public void send(TradeEventDTO dto) {
    try {
      kafkaTemplate.send("trade-events", mapper.writeValueAsString(dto));
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }
}
