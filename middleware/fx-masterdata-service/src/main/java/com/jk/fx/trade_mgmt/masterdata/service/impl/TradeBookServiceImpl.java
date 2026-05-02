package com.jk.fx.trade_mgmt.masterdata.service.impl;

import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import com.jk.fx.trade_mgmt.masterdata.dto.TradeBookDTO;
import com.jk.fx.trade_mgmt.masterdata.entity.TradeBook;
import com.jk.fx.trade_mgmt.masterdata.repository.TradeBookRepository;
import com.jk.fx.trade_mgmt.masterdata.service.TradeBookService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeBookServiceImpl implements TradeBookService {

    private final TradeBookRepository repository;

    @Override
    public TradeBookDTO create(TradeBookDTO dto) {
        if (repository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Trade book code already exists: " + dto.getCode());
        }
        TradeBook saved = repository.save(toEntity(dto, null));
        return toDto(saved);
    }

    @Override
    public TradeBookDTO update(Long id, TradeBookDTO dto) {
        TradeBook existing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trade book not found: " + id));
        if (!existing.getCode().equals(dto.getCode()) && repository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Trade book code already exists: " + dto.getCode());
        }
        existing.setCode(dto.getCode());
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setRegion(dto.getRegion());
        existing.setOwner(dto.getOwner());
        existing.setActive(dto.isActive());
        return toDto(repository.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public TradeBookDTO get(Long id) {
        return repository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Trade book not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TradeBookDTO> list(Pageable pageable) {
        return PageResponse.of(repository.findAll(pageable).map(this::toDto));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Trade book not found: " + id);
        }
        repository.deleteById(id);
    }

    private TradeBook toEntity(TradeBookDTO dto, Long id) {
        return TradeBook.builder()
                .id(id)
                .code(dto.getCode())
                .name(dto.getName())
                .description(dto.getDescription())
                .region(dto.getRegion())
                .owner(dto.getOwner())
                .active(dto.isActive())
                .build();
    }

    private TradeBookDTO toDto(TradeBook e) {
        return TradeBookDTO.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .region(e.getRegion())
                .owner(e.getOwner())
                .active(e.isActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
