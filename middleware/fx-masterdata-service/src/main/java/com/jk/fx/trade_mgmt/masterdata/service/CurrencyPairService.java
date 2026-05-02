package com.jk.fx.trade_mgmt.masterdata.service;

import com.jk.fx.trade_mgmt.masterdata.dto.CurrencyPairDTO;
import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CurrencyPairService {
    CurrencyPairDTO create(CurrencyPairDTO dto);
    CurrencyPairDTO update(Long id, CurrencyPairDTO dto);
    CurrencyPairDTO get(Long id);
    PageResponse<CurrencyPairDTO> list(Pageable pageable);
    void delete(Long id);
}
