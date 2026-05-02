package com.jk.fx.trade_mgmt.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeBookDTO {

    private Long id;

    @NotBlank
    @Size(max = 40)
    private String code;

    @NotBlank
    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 40)
    private String region;

    @Size(max = 80)
    private String owner;

    private boolean active;

    private Instant createdAt;
    private Instant updatedAt;
}
