package com.jk.fx.trade_mgmt.service;

import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RiskCalculator {

    public String calculateRisk(TradeEventDTO trade) {

        int score = 0;

        // 1️⃣ Trade size
        if (trade.getFromAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
            score += 2;
        } else if (trade.getFromAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            score += 1;
        }

        // 2️⃣ Currency pair risk
        if (!isMajorPair(trade)) {
            score += 2;
        }

        // 3️⃣ Rate sanity (basic check)
        if (trade.getRate().compareTo(BigDecimal.valueOf(100)) > 0) {
            score += 1;
        }

        // Final classification
        if (score >= 4) return "HIGH";
        if (score >= 2) return "MEDIUM";
        return "LOW";
    }

    private boolean isMajorPair(TradeEventDTO trade) {
        return trade.getFromCurrency().equals("USD")
                || trade.getToCurrency().equals("USD");
    }
}