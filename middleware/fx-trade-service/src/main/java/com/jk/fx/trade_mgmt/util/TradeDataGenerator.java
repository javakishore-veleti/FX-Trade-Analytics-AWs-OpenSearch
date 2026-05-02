
package com.jk.fx.trade_mgmt.util;

import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import com.jk.fx.trade_mgmt.kafka.TradeProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TradeDataGenerator implements CommandLineRunner {

    private final TradeProducer producer;
    private final Random random = new Random();

    @Override
    public void run(String... args) {

        for (int i = 0; i < 50; i++) {

            TradeEventDTO trade = new TradeEventDTO();

            trade.setTradeId(UUID.randomUUID().toString());
            trade.setTraderBook("FX-BOOK");

            trade.setFromCurrency("USD");
            trade.setToCurrency(random.nextBoolean() ? "INR" : "JPY");

            trade.setFromAmount(BigDecimal.valueOf(random.nextInt(200000)));
            trade.setToAmount(BigDecimal.valueOf(random.nextInt(10000000)));

            trade.setRate(BigDecimal.valueOf(80 + random.nextInt(20)));
            trade.setRegion(random.nextBoolean() ? "us-east-1" : "eu-west-1");

            producer.send(trade);
        }

        System.out.println("Generated trades");
    }
}
