package com.jk.fx.trade_mgmt.masterdata.service.impl;

import com.jk.fx.trade_mgmt.masterdata.dto.CurrencyDTO;
import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import com.jk.fx.trade_mgmt.masterdata.entity.Currency;
import com.jk.fx.trade_mgmt.masterdata.repository.CurrencyRepository;
import com.jk.fx.trade_mgmt.masterdata.service.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CurrencyServiceImpl implements CurrencyService {

    private final CurrencyRepository repository;

    @Override
    public CurrencyDTO create(CurrencyDTO dto) {
        String code = dto.getCode().toUpperCase();
        if (repository.existsById(code)) {
            throw new IllegalArgumentException("Currency already exists: " + code);
        }
        Currency saved = repository.save(toEntity(dto, code));
        return toDto(saved);
    }

    @Override
    public CurrencyDTO update(String code, CurrencyDTO dto) {
        Currency existing = repository.findById(code.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Currency not found: " + code));
        existing.setName(dto.getName());
        existing.setCountry(dto.getCountry());
        existing.setActive(dto.isActive());
        return toDto(repository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyDTO get(String code) {
        return repository.findById(code.toUpperCase())
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Currency not found: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CurrencyDTO> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable).map(this::toDto));
    }

    @Override
    public void delete(String code) {
        String key = code.toUpperCase();
        if (!repository.existsById(key)) {
            throw new EntityNotFoundException("Currency not found: " + code);
        }
        repository.deleteById(key);
    }

    private Currency toEntity(CurrencyDTO dto, String code) {
        return Currency.builder()
                .code(code)
                .name(dto.getName())
                .country(dto.getCountry())
                .active(dto.isActive())
                .build();
    }

    private CurrencyDTO toDto(Currency e) {
        return CurrencyDTO.builder()
                .code(e.getCode())
                .name(e.getName())
                .country(e.getCountry())
                .active(e.isActive())
                .build();
    }
}
