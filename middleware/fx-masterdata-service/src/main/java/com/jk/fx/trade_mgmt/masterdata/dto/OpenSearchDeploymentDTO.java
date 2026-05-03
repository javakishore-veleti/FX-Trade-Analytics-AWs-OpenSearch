package com.jk.fx.trade_mgmt.masterdata.dto;

import com.jk.fx.trade_mgmt.masterdata.entity.OpenSearchDeployment;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenSearchDeploymentDTO {

    private Long id;
    private String cloudProvider;
    private String provisionType;
    private String deploymentName;
    private String region;
    private String status;
    private String configJson;
    private Instant createdOn;
    private Instant updatedOn;
    private Instant syncedOn;

    /** Convenience field rendered by the admin UI as a clickable link. */
    private String dashboardsUrl;

    /** Convenience field — direct link into the AWS Console for this domain/collection. */
    private String awsConsoleUrl;

    public static OpenSearchDeploymentDTO of(OpenSearchDeployment e, String endpoint) {
        OpenSearchDeploymentDTO dto = OpenSearchDeploymentDTO.builder()
                .id(e.getId())
                .cloudProvider(e.getCloudProvider())
                .provisionType(e.getProvisionType())
                .deploymentName(e.getDeploymentName())
                .region(e.getRegion())
                .status(e.getStatus())
                .configJson(e.getConfigJson())
                .createdOn(e.getCreatedOn())
                .updatedOn(e.getUpdatedOn())
                .syncedOn(e.getSyncedOn())
                .build();
        if (endpoint != null && !endpoint.isBlank()) {
            // OpenSearch managed: /_dashboards path. Serverless: separate dashboards endpoint
            // (collection has its own dashboards URL stored in the config). For managed we
            // synthesize from the domain endpoint.
            if ("managed".equals(e.getProvisionType())) {
                String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
                dto.setDashboardsUrl(base + "/_dashboards");
            } else {
                // serverless dashboards URL is in the collection config; UI reads it from configJson if needed
                dto.setDashboardsUrl(null);
            }
        }
        // AWS Console deep link
        if ("aws".equals(e.getCloudProvider())) {
            if ("managed".equals(e.getProvisionType())) {
                dto.setAwsConsoleUrl(String.format(
                        "https://%s.console.aws.amazon.com/aos/home?region=%s#opensearch/domains/%s",
                        e.getRegion(), e.getRegion(), e.getDeploymentName()));
            } else if ("serverless".equals(e.getProvisionType())) {
                dto.setAwsConsoleUrl(String.format(
                        "https://%s.console.aws.amazon.com/aos/home?region=%s#opensearch/serverless/collections/%s",
                        e.getRegion(), e.getRegion(), e.getDeploymentName()));
            }
        }
        return dto;
    }
}
