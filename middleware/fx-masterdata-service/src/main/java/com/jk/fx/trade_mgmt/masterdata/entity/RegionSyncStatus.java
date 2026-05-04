package com.jk.fx.trade_mgmt.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-region last-sync record. One row per AWS region; upserted at the end
 * of each {@code syncRegion} / {@code syncAll}, even when 0 deployments
 * were discovered. The admin UI uses these rows to render "we scanned
 * us-east-1 12 minutes ago, found 1 managed domain, OK" — including for
 * regions that have no provisioned deployments yet.
 */
@Entity
@Table(
    name = "region_sync_status",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_region_sync_status_region",
        columnNames = {"region"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionSyncStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String region;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    /** {@code OK} | {@code ERROR}. */
    @Column(name = "last_status", nullable = false, length = 20)
    private String lastStatus;

    @Column(name = "managed_count", nullable = false)
    private int managedCount;

    @Column(name = "serverless_count", nullable = false)
    private int serverlessCount;

    @Column(name = "deployments_updated", nullable = false)
    private int deploymentsUpdated;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Instant createdOn;

    @Column(name = "updated_on", nullable = false)
    private Instant updatedOn;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdOn == null) createdOn = now;
        updatedOn = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedOn = Instant.now();
    }
}
