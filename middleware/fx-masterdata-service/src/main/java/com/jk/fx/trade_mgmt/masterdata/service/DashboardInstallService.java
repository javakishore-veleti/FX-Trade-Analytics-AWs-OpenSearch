package com.jk.fx.trade_mgmt.masterdata.service;

import java.util.List;

/**
 * Installs OpenSearch Dashboards saved objects (NDJSON exports) into a tracked
 * deployment. Templates ship in {@code src/main/resources/dashboards/} and are
 * loaded from the classpath at runtime so the artifact is self-contained.
 *
 * <p>The install path POSTs the NDJSON to
 * {@code <endpoint>/_dashboards/api/saved_objects/_import?overwrite=true} —
 * an OpenSearch Dashboards endpoint, not the OpenSearch data API. That has
 * implications for AWS managed clusters: it currently only works against
 * deployments that allow programmatic Dashboards access (local, or AWS
 * managed with FGAC + master-user basic auth). AWS managed without FGAC
 * returns the same anonymous-not-authorized error you'd see hitting
 * {@code /_dashboards} from a browser.
 */
public interface DashboardInstallService {

    /** Install every NDJSON template found on the classpath into the deployment with this id. */
    InstallResult installAll(Long deploymentId);

    record TemplateResult(String template, boolean ok, String message) {}

    record InstallResult(Long deploymentId, String endpoint, int templatesAttempted,
                         int templatesSucceeded, List<TemplateResult> results) {}
}
