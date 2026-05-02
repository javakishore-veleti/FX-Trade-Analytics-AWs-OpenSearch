package com.jk.fx.trade_mgmt.masterdata.service;

import com.jk.fx.trade_mgmt.masterdata.dto.CurrencyDTO;
import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CurrencyService {
    CurrencyDTO create(CurrencyDTO dto);
    CurrencyDTO update(String code, CurrencyDTO dto);
    CurrencyDTO get(String code);
    PageResponse<CurrencyDTO> list(Pageable pageable);
    void delete(String code);
}
