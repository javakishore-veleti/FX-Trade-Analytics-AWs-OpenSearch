package com.jk.fx.trade_mgmt.masterdata.dto;

import com.jk.fx.trade_mgmt.masterdata.entity.RegionSyncStatus;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionSyncStatusDTO {
    private Long id;
    private String region;
    private Instant lastSyncedAt;
    private String lastStatus;
    private int managedCount;
    private int serverlessCount;
    private int deploymentsUpdated;
    private String errorMessage;

    public static RegionSyncStatusDTO of(RegionSyncStatus e) {
        return RegionSyncStatusDTO.builder()
                .id(e.getId())
                .region(e.getRegion())
                .lastSyncedAt(e.getLastSyncedAt())
                .lastStatus(e.getLastStatus())
                .managedCount(e.getManagedCount())
                .serverlessCount(e.getServerlessCount())
                .deploymentsUpdated(e.getDeploymentsUpdated())
                .errorMessage(e.getErrorMessage())
                .build();
    }
}
