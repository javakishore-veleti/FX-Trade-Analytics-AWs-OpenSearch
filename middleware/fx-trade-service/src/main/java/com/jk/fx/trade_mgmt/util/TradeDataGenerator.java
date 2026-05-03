package com.jk.fx.trade_mgmt.util;

import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import com.jk.fx.trade_mgmt.kafka.TradeProducer;
import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fires 50 random trades on startup. Gated behind the {@code seed-data}
 * Spring profile so it doesn't run in normal local-dev or production —
 * the customer portal's "Generate demo trades" button is the regular path
 * for populating data.
 *
 * <p>Activate with: {@code --spring.profiles.active=seed-data} (e.g. for
 * smoke-testing the pipeline end-to-end without the UI).
 */
@Slf4j
@Component
@Profile("seed-data")
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
        log.info("seed-data profile: generated 50 sample trades.");
    }
}
