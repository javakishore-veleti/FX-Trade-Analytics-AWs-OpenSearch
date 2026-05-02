
package com.jk.fx.trade_mgmt.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.jk.fx.trade_mgmt.kafka.TradeProducer;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TradeService {

  private final TradeProducer producer;

  public void process() {
    TradeEventDTO dto = new TradeEventDTO();
    dto.setTradeId("T1");
    dto.setTraderBook("BOOK1");
    dto.setFromCurrency("USD");
    dto.setToCurrency("INR");
    dto.setFromAmount(BigDecimal.valueOf(100));
    dto.setToAmount(BigDecimal.valueOf(8300));
    dto.setRate(BigDecimal.valueOf(83));
    dto.setRegion("us-east-1");
    dto.setTimestamp(System.currentTimeMillis());

    producer.send(dto);
  }
}
