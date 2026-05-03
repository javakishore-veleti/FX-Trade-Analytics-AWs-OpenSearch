package com.jk.fx.trade_mgmt.masterdata.repository;

import com.jk.fx.trade_mgmt.masterdata.entity.OpenSearchDeployment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpenSearchDeploymentRepository extends JpaRepository<OpenSearchDeployment, Long> {

    Optional<OpenSearchDeployment> findByCloudProviderAndProvisionTypeAndDeploymentNameAndRegion(
            String cloudProvider, String provisionType, String deploymentName, String region);

    List<OpenSearchDeployment> findByRegion(String region);

    List<OpenSearchDeployment> findByCloudProviderOrderByRegionAscDeploymentNameAsc(String cloudProvider);

    List<OpenSearchDeployment> findAllByOrderByRegionAscDeploymentNameAsc();
}
