package com.jk.fx.trade_mgmt.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.fx.trade_mgmt.client.CurrencyPairAllowList;
import com.jk.fx.trade_mgmt.dto.TradeEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final CurrencyPairAllowList allowList;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @return true if the trade was published, false if rejected by master-data validation.
     */
    public boolean send(TradeEventDTO dto) {
        if (!allowList.isAllowed(dto.getFromCurrency(), dto.getToCurrency())) {
            log.warn("Rejected trade {}: pair {}/{} not in master-data allow-list",
                    dto.getTradeId(), dto.getFromCurrency(), dto.getToCurrency());
            return false;
        }
        try {
            kafkaTemplate.send("trade-events", mapper.writeValueAsString(dto));
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
