package com.jk.fx.trade_mgmt.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class MasterDataClient {

    private final RestTemplate masterDataRestTemplate;

    @Value("${masterdata.base-url}")
    private String baseUrl;

    public List<CurrencyPairView> fetchActiveCurrencyPairs() {
        String url = baseUrl + "/api/master/currency-pairs?size=500";
        try {
            ResponseEntity<PageResponseView<CurrencyPairView>> response =
                    masterDataRestTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {});

            PageResponseView<CurrencyPairView> body = response.getBody();
            if (body == null || body.getContent() == null) {
                return Collections.emptyList();
            }
            return body.getContent().stream().filter(CurrencyPairView::isActive).toList();
        } catch (Exception e) {
            log.warn("Failed to fetch currency pairs from master-data service ({}): {}", url, e.getMessage());
            throw new MasterDataUnavailableException(e);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrencyPairView {
        private Long id;
        private String fromCurrency;
        private String toCurrency;
        private boolean active;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageResponseView<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }

    public static class MasterDataUnavailableException extends RuntimeException {
        public MasterDataUnavailableException(Throwable cause) { super(cause); }
    }
}
