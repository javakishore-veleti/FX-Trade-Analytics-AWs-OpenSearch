package com.jk.fx.trade_mgmt.masterdata.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jk.fx.trade_mgmt.masterdata.config.AwsClientsConfig.AwsProperties;
import com.jk.fx.trade_mgmt.masterdata.dto.OpenSearchDeploymentDTO;
import com.jk.fx.trade_mgmt.masterdata.entity.OpenSearchDeployment;
import com.jk.fx.trade_mgmt.masterdata.repository.OpenSearchDeploymentRepository;
import com.jk.fx.trade_mgmt.masterdata.service.OpenSearchDeploymentService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.opensearch.OpenSearchClient;
import software.amazon.awssdk.services.opensearch.model.DescribeDomainResponse;
import software.amazon.awssdk.services.opensearch.model.DomainStatus;
import software.amazon.awssdk.services.opensearch.model.ListDomainNamesResponse;
import software.amazon.awssdk.services.opensearchserverless.OpenSearchServerlessClient;
import software.amazon.awssdk.services.opensearchserverless.model.BatchGetCollectionResponse;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionDetail;
import software.amazon.awssdk.services.opensearchserverless.model.CollectionSummary;
import software.amazon.awssdk.services.opensearchserverless.model.ListCollectionsResponse;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OpenSearchDeploymentServiceImpl implements OpenSearchDeploymentService {

    private static final String CLOUD_AWS         = "aws";
    private static final String TYPE_MANAGED      = "managed";
    private static final String TYPE_SERVERLESS   = "serverless";

    private final OpenSearchDeploymentRepository repo;
    private final AwsCredentialsProvider awsCredentials;
    private final AwsProperties awsProps;
    private final ObjectMapper jsonMapper;

    @Override
    @Transactional(readOnly = true)
    public List<OpenSearchDeploymentDTO> list() {
        return repo.findAllByOrderByRegionAscDeploymentNameAsc().stream()
                .map(e -> OpenSearchDeploymentDTO.of(e, extractEndpoint(e)))
                .toList();
    }

    @Override
    public SyncResult syncRegion(String region) {
        return syncRegions(List.of(region));
    }

    @Override
    public SyncResult syncAll() {
        if (awsProps.getRegions() == null || awsProps.getRegions().isEmpty()) {
            log.warn("syncAll: fx.aws.regions is empty — nothing to sync.");
            return new SyncResult(0, 0, 0, 0, list(), List.of("fx.aws.regions is empty in application config"));
        }
        return syncRegions(awsProps.getRegions());
    }

    private SyncResult syncRegions(List<String> regions) {
        int discovered = 0;
        int updated = 0;
        int markedInactive = 0;
        List<String> errors = new ArrayList<>();

        for (String regionCode : regions) {
            Set<String> seenManaged = new HashSet<>();
            Set<String> seenServerless = new HashSet<>();

            try {
                int n = syncManagedDomains(regionCode, seenManaged);
                discovered += n;
                updated += n;
            } catch (Exception ex) {
                String msg = "Managed domain sync failed for " + regionCode + ": " + ex.getMessage();
                log.warn(msg, ex);
                errors.add(msg);
            }

            try {
                int n = syncServerlessCollections(regionCode, seenServerless);
                discovered += n;
                updated += n;
            } catch (Exception ex) {
                String msg = "Serverless sync failed for " + regionCode + ": " + ex.getMessage();
                log.warn(msg, ex);
                errors.add(msg);
            }

            // Mark stale rows INACTIVE — they were here before, AWS no longer reports them.
            try {
                markedInactive += markStale(regionCode, seenManaged, seenServerless);
            } catch (Exception ex) {
                log.warn("Failed to mark stale rows inactive for region {}: {}", regionCode, ex.getMessage(), ex);
            }
        }

        log.info("Sync done: regions={} discovered={} updated={} markedInactive={} errors={}",
                regions.size(), discovered, updated, markedInactive, errors.size());
        return new SyncResult(regions.size(), discovered, updated, markedInactive, list(), errors);
    }

    /** AWS managed OpenSearch domains in the given region. */
    private int syncManagedDomains(String regionCode, Set<String> seenNames) {
        Region region = Region.of(regionCode);
        try (OpenSearchClient client = OpenSearchClient.builder()
                .credentialsProvider(awsCredentials)
                .region(region)
                .build()) {

            ListDomainNamesResponse list = client.listDomainNames(b -> {});
            int count = 0;
            for (var info : list.domainNames()) {
                String name = info.domainName();
                seenNames.add(name);
                DescribeDomainResponse desc = client.describeDomain(b -> b.domainName(name));
                DomainStatus status = desc.domainStatus();
                String mappedStatus = mapManagedStatus(status);
                String json = serialize(status.toString());
                upsert(CLOUD_AWS, TYPE_MANAGED, name, regionCode, mappedStatus, json);
                count++;
            }
            log.info("Region {}: discovered {} managed OpenSearch domain(s)", regionCode, count);
            return count;
        }
    }

    /** AWS OpenSearch Serverless collections in the given region. */
    private int syncServerlessCollections(String regionCode, Set<String> seenNames) {
        Region region = Region.of(regionCode);
        try (OpenSearchServerlessClient client = OpenSearchServerlessClient.builder()
                .credentialsProvider(awsCredentials)
                .region(region)
                .build()) {

            ListCollectionsResponse list = client.listCollections(b -> {});
            int count = 0;
            List<CollectionSummary> summaries = list.collectionSummaries();
            if (summaries.isEmpty()) {
                log.debug("Region {}: no serverless collections", regionCode);
                return 0;
            }

            BatchGetCollectionResponse details = client.batchGetCollection(b -> b
                    .ids(summaries.stream().map(CollectionSummary::id).toList()));
            for (CollectionDetail c : details.collectionDetails()) {
                String name = c.name();
                seenNames.add(name);
                String mappedStatus = mapServerlessStatus(c);
                String json = serialize(c.toString());
                upsert(CLOUD_AWS, TYPE_SERVERLESS, name, regionCode, mappedStatus, json);
                count++;
            }
            log.info("Region {}: discovered {} serverless collection(s)", regionCode, count);
            return count;
        }
    }

    private void upsert(String cloud, String type, String name, String region, String status, String configJson) {
        OpenSearchDeployment row = repo
                .findByCloudProviderAndProvisionTypeAndDeploymentNameAndRegion(cloud, type, name, region)
                .orElseGet(() -> OpenSearchDeployment.builder()
                        .cloudProvider(cloud)
                        .provisionType(type)
                        .deploymentName(name)
                        .region(region)
                        .build());
        row.setStatus(status);
        row.setConfigJson(configJson);
        row.setSyncedOn(Instant.now());
        repo.save(row);
    }

    /** Anything we previously had for this region that isn't in the latest scan → INACTIVE. */
    private int markStale(String region, Set<String> seenManaged, Set<String> seenServerless) {
        int marked = 0;
        for (OpenSearchDeployment row : repo.findByRegion(region)) {
            boolean stillThere = switch (row.getProvisionType()) {
                case TYPE_MANAGED    -> seenManaged.contains(row.getDeploymentName());
                case TYPE_SERVERLESS -> seenServerless.contains(row.getDeploymentName());
                default              -> true;  // local rows etc. are unaffected by AWS sync
            };
            if (!stillThere && !"INACTIVE".equals(row.getStatus())) {
                row.setStatus("INACTIVE");
                row.setSyncedOn(Instant.now());
                repo.save(row);
                marked++;
            }
        }
        return marked;
    }

    private static String mapManagedStatus(DomainStatus s) {
        if (Boolean.TRUE.equals(s.deleted()))                 return "INACTIVE";
        if (Boolean.TRUE.equals(s.processing()))              return "PROCESSING";
        if (s.domainProcessingStatus() != null
                && s.domainProcessingStatus().toString().contains("Failed")) return "ERROR";
        return Boolean.TRUE.equals(s.created()) ? "ACTIVE" : "PROCESSING";
    }

    private static String mapServerlessStatus(CollectionDetail c) {
        if (c.status() == null) return "PROCESSING";
        return switch (c.status()) {
            case ACTIVE   -> "ACTIVE";
            case CREATING -> "PROCESSING";
            case DELETING -> "PROCESSING";
            case FAILED   -> "ERROR";
            default       -> "PROCESSING";
        };
    }

    private String serialize(String fallback) {
        // AWS SDK objects don't serialize cleanly via Jackson out of the box (they're
        // not POJOs). The toString() representation is verbose but human-readable and
        // good enough for the admin view + future programmatic parsing if we need it.
        try {
            return jsonMapper.writeValueAsString(java.util.Map.of("toString", fallback));
        } catch (JsonProcessingException e) {
            return "{\"toString\":\"\"}";
        }
    }

    private static String extractEndpoint(OpenSearchDeployment e) {
        // The configJson currently wraps a toString blob. This is a placeholder hook —
        // when we move to first-class endpoint extraction, parse from configJson here
        // (DomainStatus.endpoint for managed; CollectionDetail.collectionEndpoint for
        // serverless). Until then the controller / DTO doesn't render the link.
        return null;
    }
}
