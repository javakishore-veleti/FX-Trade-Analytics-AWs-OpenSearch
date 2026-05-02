package com.jk.fx.trade_mgmt.masterdata.api;

import com.jk.fx.trade_mgmt.masterdata.dto.CurrencyDTO;
import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import com.jk.fx.trade_mgmt.masterdata.service.CurrencyService;
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
@RequestMapping("/api/master/currencies")
@RequiredArgsConstructor
@Tag(name = "Currencies", description = "Currency master data CRUD")
public class CurrencyController {

    private final CurrencyService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a currency")
    public CurrencyDTO create(@Valid @RequestBody CurrencyDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{code}")
    @Operation(summary = "Update a currency by ISO code")
    public CurrencyDTO update(@PathVariable String code, @Valid @RequestBody CurrencyDTO dto) {
        return service.update(code, dto);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a currency by ISO code")
    public CurrencyDTO get(@PathVariable String code) {
        return service.get(code);
    }

    @GetMapping
    @Operation(summary = "List currencies (paginated)")
    public PageResponse<CurrencyDTO> list(@ParameterObject Pageable pageable) {
        return service.list(pageable);
    }

    @DeleteMapping("/{code}")
    @Operation(summary = "Delete a currency by ISO code")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }
}
