package com.jk.fx.trade_mgmt.masterdata.repository;

import com.jk.fx.trade_mgmt.masterdata.entity.TradeBook;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeBookRepository extends JpaRepository<TradeBook, Long> {
    Optional<TradeBook> findByCode(String code);
    boolean existsByCode(String code);
}
