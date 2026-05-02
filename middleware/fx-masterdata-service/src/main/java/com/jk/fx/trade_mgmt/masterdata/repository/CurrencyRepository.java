package com.jk.fx.trade_mgmt.masterdata.repository;

import com.jk.fx.trade_mgmt.masterdata.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
}
