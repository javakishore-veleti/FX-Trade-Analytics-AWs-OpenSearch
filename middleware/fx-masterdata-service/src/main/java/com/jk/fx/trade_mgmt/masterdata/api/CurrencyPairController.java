package com.jk.fx.trade_mgmt.masterdata.api;

import com.jk.fx.trade_mgmt.masterdata.dto.CurrencyPairDTO;
import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import com.jk.fx.trade_mgmt.masterdata.service.CurrencyPairService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/master/currency-pairs")
@RequiredArgsConstructor
@Tag(name = "Currency Pairs", description = "Allowed FX trading pairs")
public class CurrencyPairController {

    private final CurrencyPairService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a currency pair")
    public CurrencyPairDTO create(@Valid @RequestBody CurrencyPairDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a currency pair")
    public CurrencyPairDTO update(@PathVariable Long id, @Valid @RequestBody CurrencyPairDTO dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a currency pair by id")
    public CurrencyPairDTO get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List currency pairs (paginated)")
    public PageResponse<CurrencyPairDTO> list(@ParameterObject Pageable pageable) {
        return service.list(pageable);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a currency pair")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
