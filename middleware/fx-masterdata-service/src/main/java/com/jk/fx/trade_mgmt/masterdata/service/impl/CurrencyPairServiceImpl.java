package com.jk.fx.trade_mgmt.masterdata.service.impl;

import com.jk.fx.trade_mgmt.masterdata.dto.CurrencyPairDTO;
import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import com.jk.fx.trade_mgmt.masterdata.entity.CurrencyPair;
import com.jk.fx.trade_mgmt.masterdata.repository.CurrencyPairRepository;
import com.jk.fx.trade_mgmt.masterdata.repository.CurrencyRepository;
import com.jk.fx.trade_mgmt.masterdata.service.CurrencyPairService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CurrencyPairServiceImpl implements CurrencyPairService {

    private final CurrencyPairRepository repository;
    private final CurrencyRepository currencyRepository;

    @Override
    public CurrencyPairDTO create(CurrencyPairDTO dto) {
        String from = dto.getFromCurrency().toUpperCase();
        String to = dto.getToCurrency().toUpperCase();
        validateCurrencies(from, to);
        repository.findByFromCurrencyAndToCurrency(from, to).ifPresent(p -> {
            throw new IllegalArgumentException("Currency pair already exists: " + from + "/" + to);
        });
        CurrencyPair saved = repository.save(CurrencyPair.builder()
                .fromCurrency(from)
                .toCurrency(to)
                .active(dto.isActive())
                .build());
        return toDto(saved);
    }

    @Override
    public CurrencyPairDTO update(Long id, CurrencyPairDTO dto) {
        CurrencyPair existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Currency pair not found: " + id));
        String from = dto.getFromCurrency().toUpperCase();
        String to = dto.getToCurrency().toUpperCase();
        validateCurrencies(from, to);
        existing.setFromCurrency(from);
        existing.setToCurrency(to);
        existing.setActive(dto.isActive());
        return toDto(repository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyPairDTO get(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Currency pair not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CurrencyPairDTO> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable).map(this::toDto));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Currency pair not found: " + id);
        }
        repository.deleteById(id);
    }

    private void validateCurrencies(String from, String to) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("from and to currencies must differ");
        }
        if (!currencyRepository.existsById(from)) {
            throw new IllegalArgumentException("Unknown currency: " + from);
        }
        if (!currencyRepository.existsById(to)) {
            throw new IllegalArgumentException("Unknown currency: " + to);
        }
    }

    private CurrencyPairDTO toDto(CurrencyPair e) {
        return CurrencyPairDTO.builder()
                .id(e.getId())
                .fromCurrency(e.getFromCurrency())
                .toCurrency(e.getToCurrency())
                .active(e.isActive())
                .build();
    }
}
