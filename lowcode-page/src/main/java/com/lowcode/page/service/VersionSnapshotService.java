package com.lowcode.page.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lowcode.page.dto.VersionSnapshotDTO;
import com.lowcode.page.entity.VersionSnapshot;
import com.lowcode.page.vo.VersionDiffVO;

import java.util.List;

public interface VersionSnapshotService extends IService<VersionSnapshot> {

    VersionSnapshot createSnapshot(VersionSnapshotDTO dto);

    List<VersionSnapshot> getSnapshotList(Long resourceId, String resourceType, Long appId);

    VersionSnapshot getSnapshotDetail(Long id);

    VersionSnapshot rollbackToSnapshot(Long snapshotId, String reason, Boolean createNewSnapshot);

    VersionDiffVO compareVersions(Long oldSnapshotId, Long newSnapshotId);

    VersionSnapshot createAutoSnapshot(String resourceType, Long resourceId, Long appId);
}
