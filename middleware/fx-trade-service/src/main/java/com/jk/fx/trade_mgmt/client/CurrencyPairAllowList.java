package com.jk.fx.trade_mgmt.client;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrencyPairAllowList {

    private final MasterDataClient client;

    @Value("${masterdata.allow-list.fail-open:true}")
    private boolean failOpen;

    @Value("${masterdata.allow-list.refresh-interval-seconds:300}")
    private long refreshIntervalSeconds;

    private final Set<String> allowed = ConcurrentHashMap.newKeySet();
    private final AtomicLong lastRefreshEpochSec = new AtomicLong(0);
    private volatile boolean lastRefreshSucceeded = false;

    @PostConstruct
    void init() {
        refresh();
    }

    public boolean isAllowed(String fromCurrency, String toCurrency) {
        if (fromCurrency == null || toCurrency == null) return false;
        maybeRefresh();
        if (allowed.isEmpty() && !lastRefreshSucceeded && failOpen) {
            // Master-data unreachable and never populated — let trades through (dev convenience).
            return true;
        }
        return allowed.contains(key(fromCurrency, toCurrency));
    }

    public synchronized void refresh() {
        try {
            var pairs = client.fetchActiveCurrencyPairs();
            allowed.clear();
            pairs.forEach(p -> allowed.add(key(p.getFromCurrency(), p.getToCurrency())));
            lastRefreshSucceeded = true;
            lastRefreshEpochSec.set(System.currentTimeMillis() / 1000);
            log.info("Loaded {} active currency pairs from master-data", allowed.size());
        } catch (MasterDataClient.MasterDataUnavailableException e) {
            lastRefreshSucceeded = false;
            log.warn("Master-data refresh failed; fail-open={}, cached pairs={}", failOpen, allowed.size());
        }
    }

    private void maybeRefresh() {
        long now = System.currentTimeMillis() / 1000;
        if (now - lastRefreshEpochSec.get() >= refreshIntervalSeconds) {
            refresh();
        }
    }

    private static String key(String from, String to) {
        return from.toUpperCase() + "/" + to.toUpperCase();
    }
}
