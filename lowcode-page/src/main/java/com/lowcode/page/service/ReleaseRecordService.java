package com.lowcode.page.service;

import com.lowcode.page.dto.ReleaseRecordDTO;
import com.lowcode.page.entity.ReleaseRecord;

import java.util.List;

public interface ReleaseRecordService {

    ReleaseRecord createRelease(ReleaseRecordDTO dto);

    List<ReleaseRecord> getReleaseList(Long resourceId, String resourceType, Long appId);

    ReleaseRecord getReleaseDetail(Long id);

    ReleaseRecord publishRelease(Long id);

    ReleaseRecord rollbackRelease(Long id, String reason);

    ReleaseRecord stopGrayRelease(Long id);

    ReleaseRecord cancelGrayRelease(Long id);
}
