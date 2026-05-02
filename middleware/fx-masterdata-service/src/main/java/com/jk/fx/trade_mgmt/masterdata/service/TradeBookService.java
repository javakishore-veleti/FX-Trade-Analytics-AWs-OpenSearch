package com.jk.fx.trade_mgmt.masterdata.service;

import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import com.jk.fx.trade_mgmt.masterdata.dto.TradeBookDTO;
import org.springframework.data.domain.Pageable;

public interface TradeBookService {
    TradeBookDTO create(TradeBookDTO dto);
    TradeBookDTO update(Long id, TradeBookDTO dto);
    TradeBookDTO get(Long id);
    PageResponse<TradeBookDTO> list(Pageable pageable);
    void delete(Long id);
}
