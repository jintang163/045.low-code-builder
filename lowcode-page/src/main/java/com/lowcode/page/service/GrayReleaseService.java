package com.lowcode.page.service;

import com.lowcode.page.entity.GrayReleaseConfig;
import com.lowcode.page.vo.GrayReleaseResultVO;

public interface GrayReleaseService {

    GrayReleaseConfig createGrayConfig(GrayReleaseConfig config);

    GrayReleaseResultVO checkGrayRelease(Long resourceId, String resourceType, Long userId, String userGroup);

    GrayReleaseConfig getActiveGrayConfig(Long resourceId, String resourceType);

    GrayReleaseConfig stopGrayRelease(Long configId);

    GrayReleaseConfig cancelGrayRelease(Long configId);
}
