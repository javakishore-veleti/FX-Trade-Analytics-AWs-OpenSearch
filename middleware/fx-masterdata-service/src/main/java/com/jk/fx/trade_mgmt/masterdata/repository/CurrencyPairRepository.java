package com.jk.fx.trade_mgmt.masterdata.repository;

import com.jk.fx.trade_mgmt.masterdata.entity.CurrencyPair;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyPairRepository extends JpaRepository<CurrencyPair, Long> {
    Optional<CurrencyPair> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
