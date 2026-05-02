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
public class CurrencyDTO {

    @NotBlank
    @Size(min = 3, max = 3)
    private String code;

    @NotBlank
    @Size(max = 80)
    private String name;

    @Size(max = 80)
    private String country;

    private boolean active;
}
