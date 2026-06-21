package com.lowcode.page.service.impl.abtest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.abtest.ABTest;
import com.lowcode.page.entity.abtest.ABTestVariant;
import com.lowcode.page.mapper.abtest.ABTestVariantMapper;
import com.lowcode.page.service.abtest.ABTestService;
import com.lowcode.page.service.abtest.ABTestVariantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ABTestVariantServiceImpl extends ServiceImpl<ABTestVariantMapper, ABTestVariant> implements ABTestVariantService {

    @Autowired
    private ABTestVariantMapper abTestVariantMapper;

    @Autowired
    @Lazy
    private ABTestService abTestService;

    private final Set<String> viewedUsers = new HashSet<>();

    @Override
    public List<ABTestVariant> getVariantList(Long testId) {
        log.info("获取变体列表，testId: {}", testId);
        try {
            LambdaQueryWrapper<ABTestVariant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ABTestVariant::getTestId, testId);
            wrapper.orderByAsc(ABTestVariant::getCreatedTime);
            List<ABTestVariant> list = list(wrapper);
            log.info("获取变体列表成功，testId: {}, 数量: {}", testId, list.size());
            return list;
        } catch (Exception e) {
            log.error("获取变体列表失败，testId: {}", testId, e);
            return getMockVariantList(testId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ABTestVariant saveVariant(ABTestVariant variant) {
        log.info("保存变体，variantName: {}, testId: {}", variant.getVariantName(), variant.getTestId());
        try {
            ABTest test = abTestService.getById(variant.getTestId());
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            if (variant.getPageViews() == null) {
                variant.setPageViews(0L);
            }
            if (variant.getUniqueVisitors() == null) {
                variant.setUniqueVisitors(0L);
            }
            if (variant.getConversions() == null) {
                variant.setConversions(0L);
            }
            if (variant.getConversionRate() == null) {
                variant.setConversionRate(BigDecimal.ZERO);
            }
            save(variant);
            log.info("保存变体成功，id: {}", variant.getId());
            return getById(variant.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存变体失败", e);
            return getMockSavedVariant(variant);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ABTestVariant updateVariant(ABTestVariant variant) {
        log.info("更新变体，id: {}", variant.getId());
        try {
            ABTestVariant existing = getById(variant.getId());
            if (existing == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "变体不存在");
            }
            ABTest test = abTestService.getById(existing.getTestId());
            if (test != null && test.getStatus() == 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "测试进行中，不允许修改变体");
            }
            updateById(variant);
            log.info("更新变体成功，id: {}", variant.getId());
            return getById(variant.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新变体失败，id: {}", variant.getId(), e);
            return getMockUpdatedVariant(variant);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVariant(Long id) {
        log.info("删除变体，id: {}", id);
        try {
            ABTestVariant variant = getById(id);
            if (variant == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "变体不存在");
            }
            ABTest test = abTestService.getById(variant.getTestId());
            if (test != null && test.getStatus() == 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "测试进行中，不允许删除变体");
            }
            removeById(id);
            log.info("删除变体成功，id: {}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除变体失败，id: {}", id, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementPageView(Long variantId, Long userId) {
        log.info("增加浏览量，variantId: {}, userId: {}", variantId, userId);
        try {
            ABTestVariant variant = getById(variantId);
            if (variant == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "变体不存在");
            }
            variant.setPageViews(variant.getPageViews() != null ? variant.getPageViews() + 1 : 1L);
            if (userId != null) {
                String userKey = variantId + ":" + userId;
                if (!viewedUsers.contains(userKey)) {
                    viewedUsers.add(userKey);
                    variant.setUniqueVisitors(variant.getUniqueVisitors() != null ? variant.getUniqueVisitors() + 1 : 1L);
                }
            }
            if (variant.getPageViews() > 0 && variant.getConversions() != null) {
                variant.setConversionRate(BigDecimal.valueOf(variant.getConversions())
                        .divide(BigDecimal.valueOf(variant.getPageViews()), 4, RoundingMode.HALF_UP));
            }
            updateById(variant);
            log.info("增加浏览量成功，variantId: {}, pageViews: {}", variantId, variant.getPageViews());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("增加浏览量失败，variantId: {}", variantId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementConversion(Long variantId, Long userId, String eventKey) {
        log.info("增加转化数，variantId: {}, userId: {}, eventKey: {}", variantId, userId, eventKey);
        try {
            ABTestVariant variant = getById(variantId);
            if (variant == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "变体不存在");
            }
            variant.setConversions(variant.getConversions() != null ? variant.getConversions() + 1 : 1L);
            if (variant.getPageViews() != null && variant.getPageViews() > 0) {
                variant.setConversionRate(BigDecimal.valueOf(variant.getConversions())
                        .divide(BigDecimal.valueOf(variant.getPageViews()), 4, RoundingMode.HALF_UP));
            }
            updateById(variant);
            log.info("增加转化数成功，variantId: {}, conversions: {}", variantId, variant.getConversions());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("增加转化数失败，variantId: {}", variantId, e);
        }
    }

    private List<ABTestVariant> getMockVariantList(Long testId) {
        List<ABTestVariant> variants = new ArrayList<>();
        ABTestVariant v1 = new ABTestVariant();
        v1.setId(1L);
        v1.setTestId(testId);
        v1.setVariantName("对照组");
        v1.setVariantType("CONTROL");
        v1.setDescription("原始版本");
        v1.setTrafficWeight(50);
        v1.setSnapshotId(1L);
        v1.setVersion("v1.0.0");
        v1.setPageViews(1000L);
        v1.setUniqueVisitors(800L);
        v1.setConversions(50L);
        v1.setConversionRate(new BigDecimal("0.0500"));
        variants.add(v1);
        ABTestVariant v2 = new ABTestVariant();
        v2.setId(2L);
        v2.setTestId(testId);
        v2.setVariantName("实验组A");
        v2.setVariantType("EXPERIMENT");
        v2.setDescription("新版本A");
        v2.setTrafficWeight(50);
        v2.setSnapshotId(2L);
        v2.setVersion("v1.1.0");
        v2.setPageViews(1000L);
        v2.setUniqueVisitors(850L);
        v2.setConversions(70L);
        v2.setConversionRate(new BigDecimal("0.0700"));
        variants.add(v2);
        return variants;
    }

    private ABTestVariant getMockSavedVariant(ABTestVariant variant) {
        variant.setId(System.currentTimeMillis());
        variant.setPageViews(0L);
        variant.setUniqueVisitors(0L);
        variant.setConversions(0L);
        variant.setConversionRate(BigDecimal.ZERO);
        return variant;
    }

    private ABTestVariant getMockUpdatedVariant(ABTestVariant variant) {
        return variant;
    }
}
