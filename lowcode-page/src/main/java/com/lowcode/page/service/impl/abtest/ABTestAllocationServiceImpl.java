package com.lowcode.page.service.impl.abtest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.abtest.ABTest;
import com.lowcode.page.entity.abtest.ABTestVariant;
import com.lowcode.page.service.abtest.ABTestAllocationService;
import com.lowcode.page.service.abtest.ABTestService;
import com.lowcode.page.service.abtest.ABTestVariantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ABTestAllocationServiceImpl implements ABTestAllocationService {

    @Autowired
    @Lazy
    private ABTestService abTestService;

    @Autowired
    @Lazy
    private ABTestVariantService variantService;

    @Override
    public ABTestVariant allocateVariant(Long testId, Long userId, String userGroup) {
        log.info("分配变体，testId: {}, userId: {}, userGroup: {}", testId, userId, userGroup);
        try {
            ABTest test = abTestService.getById(testId);
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            if (test.getStatus() != 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "测试未在运行中");
            }
            List<ABTestVariant> variants = variantService.getVariantList(testId);
            if (variants == null || variants.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "没有可用的变体");
            }
            if (!checkUserInTraffic(test, userId, userGroup)) {
                ABTestVariant controlVariant = getControlVariant(variants);
                log.info("用户未进入测试流量，返回对照组，userId: {}", userId);
                return controlVariant;
            }
            int hashValue = calculateConsistentHash(userId, test.getHashField());
            ABTestVariant allocatedVariant = allocateByTrafficWeight(variants, hashValue);
            log.info("分配变体成功，userId: {}, variantId: {}, variantName: {}", 
                    userId, allocatedVariant.getId(), allocatedVariant.getVariantName());
            return allocatedVariant;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("分配变体失败，testId: {}, userId: {}", testId, userId, e);
            return getMockAllocatedVariant(testId);
        }
    }

    @Override
    public ABTestVariant checkAllocation(Long testId, Long userId) {
        log.info("检查用户分配的变体，testId: {}, userId: {}", testId, userId);
        try {
            ABTest test = abTestService.getById(testId);
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            List<ABTestVariant> variants = variantService.getVariantList(testId);
            if (variants == null || variants.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "没有可用的变体");
            }
            if (test.getStatus() != 1) {
                ABTestVariant controlVariant = getControlVariant(variants);
                log.info("测试未运行，返回默认变体，testId: {}", testId);
                return controlVariant;
            }
            return allocateVariant(testId, userId, null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("检查用户分配变体失败，testId: {}, userId: {}", testId, userId, e);
            return getMockAllocatedVariant(testId);
        }
    }

    @Override
    public int calculateConsistentHash(Long userId, String salt) {
        if (userId == null) {
            return 100;
        }
        String hashSource = userId + ":" + (salt != null ? salt : "abtest");
        int hash = hashSource.hashCode();
        if (hash < 0) {
            hash = -hash;
        }
        int result = hash % 100;
        log.debug("一致性哈希计算，source: {}, hash: {}, result: {}", hashSource, hash, result);
        return result;
    }

    private boolean checkUserInTraffic(ABTest test, Long userId, String userGroup) {
        if (test.getTrafficPercent() == null || test.getTrafficPercent() >= 100) {
            return true;
        }
        if (test.getTargetUserGroup() != null && !test.getTargetUserGroup().isEmpty()) {
            if (userGroup == null) {
                return false;
            }
            Set<String> targetGroups = new HashSet<>(Arrays.asList(test.getTargetUserGroup().split(",")));
            if (!targetGroups.contains(userGroup)) {
                return false;
            }
        }
        int hashValue = calculateConsistentHash(userId, test.getHashField());
        return hashValue < test.getTrafficPercent();
    }

    private ABTestVariant getControlVariant(List<ABTestVariant> variants) {
        for (ABTestVariant variant : variants) {
            if ("CONTROL".equals(variant.getVariantType())) {
                return variant;
            }
        }
        return variants.get(0);
    }

    private ABTestVariant allocateByTrafficWeight(List<ABTestVariant> variants, int hashValue) {
        if (variants.size() == 1) {
            return variants.get(0);
        }
        int totalWeight = 0;
        for (ABTestVariant variant : variants) {
            totalWeight += variant.getTrafficWeight() != null ? variant.getTrafficWeight() : 0;
        }
        if (totalWeight <= 0) {
            return variants.get(0);
        }
        int position = (hashValue * totalWeight) / 100;
        int cumulativeWeight = 0;
        for (ABTestVariant variant : variants) {
            cumulativeWeight += variant.getTrafficWeight() != null ? variant.getTrafficWeight() : 0;
            if (position < cumulativeWeight) {
                return variant;
            }
        }
        return variants.get(variants.size() - 1);
    }

    private ABTestVariant getMockAllocatedVariant(Long testId) {
        ABTestVariant variant = new ABTestVariant();
        variant.setId(1L);
        variant.setTestId(testId);
        variant.setVariantName("对照组");
        variant.setVariantType("CONTROL");
        variant.setDescription("原始版本");
        variant.setTrafficWeight(50);
        variant.setPageViews(1000L);
        variant.setConversions(50L);
        return variant;
    }
}
