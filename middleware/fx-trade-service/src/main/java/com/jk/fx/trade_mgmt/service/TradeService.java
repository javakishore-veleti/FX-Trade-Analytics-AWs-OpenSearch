package com.jk.fx.trade_mgmt.service;

import com.jk.fx.trade_mgmt.api.TradePlaceResponse;
import com.jk.fx.trade_mgmt.api.TradeRequest;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import com.jk.fx.trade_mgmt.kafka.TradeProducer;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeProducer producer;

    /**
     * Legacy demo entry point: emits a hardcoded USD→INR trade.
     * Kept for backward compatibility with the existing TradeController POST /api/trades.
     */
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

    public TradePlaceResponse place(TradeRequest req) {
        String id = UUID.randomUUID().toString();
        TradeEventDTO dto = new TradeEventDTO();
        dto.setTradeId(id);
        dto.setTraderBook(req.getTraderBook() != null ? req.getTraderBook() : "FX-BOOK");
        dto.setFromCurrency(safeUpper(req.getFromCurrency()));
        dto.setToCurrency(safeUpper(req.getToCurrency()));
        dto.setFromAmount(req.getFromAmount());
        dto.setRate(req.getRate());
        dto.setToAmount(req.getFromAmount() != null && req.getRate() != null
                ? req.getFromAmount().multiply(req.getRate())
                : null);
        dto.setRegion(req.getRegion() != null ? req.getRegion() : "us-east-1");
        dto.setTimestamp(System.currentTimeMillis());

        boolean accepted = producer.send(dto);
        return new TradePlaceResponse(
                id,
                accepted,
                accepted ? "queued" : "rejected: currency pair not in master-data allow-list"
        );
    }

    private static String safeUpper(String s) {
        return s == null ? null : s.toUpperCase();
    }
}
