package com.jk.fx.trade_mgmt.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyPairDTO {

    private Long id;

    @NotBlank
    @Size(min = 3, max = 3)
    private String fromCurrency;

    @NotBlank
    @Size(min = 3, max = 3)
    private String toCurrency;

    private boolean active;
}
