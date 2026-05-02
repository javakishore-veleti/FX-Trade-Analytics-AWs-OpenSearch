package com.jk.fx.trade_mgmt.masterdata.api;

import com.jk.fx.trade_mgmt.masterdata.dto.PageResponse;
import com.jk.fx.trade_mgmt.masterdata.dto.TradeBookDTO;
import com.jk.fx.trade_mgmt.masterdata.service.TradeBookService;
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
@RequestMapping("/api/master/trade-books")
@RequiredArgsConstructor
@Tag(name = "Trade Books", description = "Trade book master data CRUD")
public class TradeBookController {

    private final TradeBookService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a trade book")
    public TradeBookDTO create(@Valid @RequestBody TradeBookDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a trade book")
    public TradeBookDTO update(@PathVariable Long id, @Valid @RequestBody TradeBookDTO dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a trade book by id")
    public TradeBookDTO get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    @Operation(summary = "List trade books (paginated)")
    public PageResponse<TradeBookDTO> list(@ParameterObject Pageable pageable) {
        return service.list(pageable);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a trade book")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
