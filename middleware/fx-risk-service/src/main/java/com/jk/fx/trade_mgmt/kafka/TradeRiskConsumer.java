package com.jk.fx.trade_mgmt.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import com.jk.fx.trade_mgmt.service.RiskCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TradeRiskConsumer {

    private final ObjectMapper mapper;
    private final RiskCalculator calculator;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "trade-events", groupId = "risk-group")
    public void consume(String message) {

        try {
            TradeEventDTO trade =
                    mapper.readValue(message, TradeEventDTO.class);

            String risk = calculator.calculateRisk(trade);
            trade.setRiskLevel(risk);

            System.out.println("⚡ Risk calculated: " + risk);

            if (trade.getFromAmount().intValue() > 50000) {
              throw new RuntimeException("Simulated failure 🔥");
            }

            // 🔥 Send enriched event forward
            kafkaTemplate.send("trade-events-enriched",
                    mapper.writeValueAsString(trade));

        } catch (Exception e) {
            System.out.println("❌ Risk processing failed");
        }
    }
}