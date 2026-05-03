package com.jk.fx.trade_mgmt.masterdata.service;

import com.jk.fx.trade_mgmt.masterdata.dto.OpenSearchDeploymentDTO;
import java.util.List;

public interface OpenSearchDeploymentService {

    /** All deployments currently known in the DB, ordered by region then name. */
    List<OpenSearchDeploymentDTO> list();

    /** Upserts deployments for a single AWS region from both managed + serverless APIs. */
    SyncResult syncRegion(String region);

    /** Calls {@link #syncRegion(String)} for every region in {@code fx.aws.regions}. */
    SyncResult syncAll();

    record SyncResult(int regionsScanned, int deploymentsDiscovered, int updated, int markedInactive,
                      List<OpenSearchDeploymentDTO> deployments,
                      List<String> errors) {}
}
