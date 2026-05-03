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
 * Tracks one logical OpenSearch deployment (managed cluster or serverless
 * collection) discovered via the AWS control-plane APIs and synced into
 * masterdata. The admin UI lists, syncs, and links into AWS Console from
 * these rows.
 */
@Entity
@Table(
    name = "opensearch_deployments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_opensearch_deployment",
        columnNames = {"cloud_provider", "provision_type", "deployment_name", "region"}
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenSearchDeployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** {@code aws} | {@code azure} | {@code gcp} | {@code local}. */
    @Column(name = "cloud_provider", nullable = false, length = 20)
    private String cloudProvider;

    /**
     * {@code managed} | {@code serverless} | {@code local-docker}. AWS managed
     * clusters and AWS Serverless collections have different APIs, endpoint
     * formats, and SigV4 service names ({@code es} vs {@code aoss}).
     */
    @Column(name = "provision_type", nullable = false, length = 20)
    private String provisionType;

    /** AWS managed: domain name. AWS serverless: collection name. */
    @Column(name = "deployment_name", nullable = false, length = 120)
    private String deploymentName;

    @Column(nullable = false, length = 40)
    private String region;

    /** {@code ACTIVE} | {@code PROCESSING} | {@code INACTIVE} | {@code ERROR}. */
    @Column(nullable = false, length = 20)
    private String status;

    /**
     * Canonical HTTPS endpoint from the AWS control-plane API.
     * Managed:    {@code DomainStatus.endpoint()} → {@code search-...amazonaws.com}.
     * Serverless: {@code CollectionDetail.collectionEndpoint()} → {@code https://....aoss.amazonaws.com}.
     * Consumed by fx-search-client when {@code fx.opensearch.source.type=masterdata}
     * to construct region-keyed OpenSearchClient instances at runtime.
     */
    @Column(name = "endpoint", length = 500)
    private String endpoint;

    /**
     * Full AWS describe-* response serialised as JSON. Debug / human-readable
     * blob — first-class fields like {@link #endpoint} should be promoted out
     * of here when callers need them programmatically.
     */
    @Column(name = "config_json", nullable = false, columnDefinition = "CLOB")
    private String configJson;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Instant createdOn;

    @Column(name = "updated_on", nullable = false)
    private Instant updatedOn;

    /** Last time we successfully fetched this deployment from AWS. */
    @Column(name = "synced_on")
    private Instant syncedOn;

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
