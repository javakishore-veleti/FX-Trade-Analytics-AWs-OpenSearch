package com.jk.fx.trade_mgmt.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import com.jk.fx.trade_mgmt.service.RiskCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeRiskConsumer {

    private final ObjectMapper mapper;
    private final RiskCalculator calculator;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "trade-events", groupId = "risk-group")
    public void consume(String message) throws Exception {
        TradeEventDTO trade = mapper.readValue(message, TradeEventDTO.class);

        String risk = calculator.calculateRisk(trade);
        trade.setRiskLevel(risk);

        log.info("⚡ Risk calculated: {} for trade {}", risk, trade.getTradeId());

        kafkaTemplate.send("trade-events-enriched", mapper.writeValueAsString(trade));
    }
}
