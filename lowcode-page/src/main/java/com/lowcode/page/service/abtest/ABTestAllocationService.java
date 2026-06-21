package com.lowcode.page.service.abtest;

import com.lowcode.page.entity.abtest.ABTestVariant;

public interface ABTestAllocationService {

    ABTestVariant allocateVariant(Long testId, Long userId, String userGroup);

    ABTestVariant checkAllocation(Long testId, Long userId);

    int calculateConsistentHash(Long userId, String salt);
}
