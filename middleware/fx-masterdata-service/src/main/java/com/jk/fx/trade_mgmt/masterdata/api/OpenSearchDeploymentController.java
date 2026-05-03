package com.jk.fx.trade_mgmt.masterdata.api;

import com.jk.fx.trade_mgmt.masterdata.dto.OpenSearchDeploymentDTO;
import com.jk.fx.trade_mgmt.masterdata.service.DashboardInstallService;
import com.jk.fx.trade_mgmt.masterdata.service.DashboardInstallService.InstallResult;
import com.jk.fx.trade_mgmt.masterdata.service.OpenSearchDeploymentService;
import com.jk.fx.trade_mgmt.masterdata.service.OpenSearchDeploymentService.SyncResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/opensearch-deployments")
@RequiredArgsConstructor
@Tag(name = "OpenSearch Deployments",
     description = "Tracks AWS OpenSearch managed clusters + serverless collections discovered via the AWS control-plane APIs")
public class OpenSearchDeploymentController {

    private final OpenSearchDeploymentService service;
    private final DashboardInstallService dashboardInstaller;

    @GetMapping
    @Operation(summary = "List all known deployments (DB read; does not call AWS).")
    public List<OpenSearchDeploymentDTO> list() {
        return service.list();
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync a single AWS region from both managed + serverless APIs and upsert.")
    public SyncResult sync(@RequestParam("region") String region) {
        return service.syncRegion(region);
    }

    @PostMapping("/sync-all")
    @Operation(summary = "Sync every region declared in fx.aws.regions.")
    public SyncResult syncAll() {
        return service.syncAll();
    }

    @PostMapping("/{id}/install-dashboards")
    @Operation(summary = "Import every NDJSON template under classpath:dashboards/ into the deployment's OpenSearch Dashboards.")
    public InstallResult installDashboards(@PathVariable("id") Long id) {
        return dashboardInstaller.installAll(id);
    }
}
