package com.jk.fx.trade_mgmt.masterdata.repository;

import com.jk.fx.trade_mgmt.masterdata.entity.RegionSyncStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionSyncStatusRepository extends JpaRepository<RegionSyncStatus, Long> {

    Optional<RegionSyncStatus> findByRegion(String region);

    List<RegionSyncStatus> findAllByOrderByRegionAsc();
}
