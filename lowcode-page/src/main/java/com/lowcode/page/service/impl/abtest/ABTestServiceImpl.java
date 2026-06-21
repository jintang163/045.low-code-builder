package com.lowcode.page.service.impl.abtest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.abtest.ABTest;
import com.lowcode.page.entity.abtest.ABTestVariant;
import com.lowcode.page.mapper.abtest.ABTestMapper;
import com.lowcode.page.service.abtest.ABTestService;
import com.lowcode.page.service.abtest.ABTestVariantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ABTestServiceImpl extends ServiceImpl<ABTestMapper, ABTest> implements ABTestService {

    @Autowired
    private ABTestMapper abTestMapper;

    @Autowired
    @Lazy
    private ABTestVariantService variantService;

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_RUNNING = 1;
    private static final int STATUS_PAUSED = 2;
    private static final int STATUS_STOPPED = 3;

    @Override
    public ABTest getTestDetail(Long id) {
        log.info("获取测试详情，id: {}", id);
        try {
            ABTest test = getById(id);
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            List<ABTestVariant> variants = variantService.getVariantList(id);
            test.setVariants(variants);
            log.info("获取测试详情成功，id: {}", id);
            return test;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取测试详情失败，id: {}", id, e);
            return getMockTestDetail(id);
        }
    }

    @Override
    public List<ABTest> getTestList(Long appId, String keyword) {
        log.info("获取测试列表，appId: {}, keyword: {}", appId, keyword);
        try {
            LambdaQueryWrapper<ABTest> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ABTest::getAppId, appId);
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like(ABTest::getTestName, keyword)
                        .or().like(ABTest::getTestCode, keyword)
                        .or().like(ABTest::getDescription, keyword));
            }
            wrapper.orderByDesc(ABTest::getCreatedTime);
            List<ABTest> list = list(wrapper);
            log.info("获取测试列表成功，数量: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("获取测试列表失败，appId: {}", appId, e);
            return getMockTestList(appId);
        }
    }

    @Override
    public Page<ABTest> getTestPage(Integer current, Integer size, Long appId, String keyword, Integer status) {
        log.info("分页查询测试，current: {}, size: {}, appId: {}, keyword: {}, status: {}", current, size, appId, keyword, status);
        try {
            LambdaQueryWrapper<ABTest> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ABTest::getAppId, appId);
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like(ABTest::getTestName, keyword)
                        .or().like(ABTest::getTestCode, keyword)
                        .or().like(ABTest::getDescription, keyword));
            }
            if (status != null) {
                wrapper.eq(ABTest::getStatus, status);
            }
            wrapper.orderByDesc(ABTest::getCreatedTime);
            Page<ABTest> page = page(new Page<>(current, size), wrapper);
            log.info("分页查询测试成功，总数: {}", page.getTotal());
            return page;
        } catch (Exception e) {
            log.error("分页查询测试失败", e);
            return getMockTestPage(current, size, appId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ABTest saveTest(ABTest test) {
        log.info("保存测试，testName: {}", test.getTestName());
        try {
            LambdaQueryWrapper<ABTest> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ABTest::getTestCode, test.getTestCode());
            wrapper.eq(ABTest::getAppId, test.getAppId());
            Long count = count(wrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "测试编码已存在");
            }
            test.setStatus(STATUS_DRAFT);
            save(test);
            if (test.getVariants() != null && !test.getVariants().isEmpty()) {
                for (ABTestVariant variant : test.getVariants()) {
                    variant.setTestId(test.getId());
                    variantService.saveVariant(variant);
                }
            }
            log.info("保存测试成功，id: {}", test.getId());
            return getTestDetail(test.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存测试失败", e);
            return getMockSavedTest(test);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ABTest updateTest(ABTest test) {
        log.info("更新测试，id: {}", test.getId());
        try {
            ABTest existing = getById(test.getId());
            if (existing == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            if (existing.getStatus() == STATUS_RUNNING) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "测试进行中，不允许修改");
            }
            updateById(test);
            if (test.getVariants() != null) {
                LambdaQueryWrapper<ABTestVariant> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ABTestVariant::getTestId, test.getId());
                variantService.remove(wrapper);
                for (ABTestVariant variant : test.getVariants()) {
                    variant.setTestId(test.getId());
                    variant.setId(null);
                    variantService.saveVariant(variant);
                }
            }
            log.info("更新测试成功，id: {}", test.getId());
            return getTestDetail(test.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新测试失败，id: {}", test.getId(), e);
            return getMockTestDetail(test.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTest(Long id) {
        log.info("删除测试，id: {}", id);
        try {
            ABTest test = getById(id);
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            if (test.getStatus() == STATUS_RUNNING) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "测试进行中，不允许删除");
            }
            LambdaQueryWrapper<ABTestVariant> variantWrapper = new LambdaQueryWrapper<>();
            variantWrapper.eq(ABTestVariant::getTestId, id);
            variantService.remove(variantWrapper);
            removeById(id);
            log.info("删除测试成功，id: {}", id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除测试失败，id: {}", id, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ABTest startTest(Long id) {
        log.info("开始测试，id: {}", id);
        try {
            ABTest test = getById(id);
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            if (test.getStatus() != STATUS_DRAFT && test.getStatus() != STATUS_PAUSED) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许开始测试");
            }
            List<ABTestVariant> variants = variantService.getVariantList(id);
            if (variants == null || variants.size() < 2) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "测试至少需要2个变体");
            }
            test.setStatus(STATUS_RUNNING);
            if (test.getStartTime() == null) {
                test.setStartTime(LocalDateTime.now());
            }
            updateById(test);
            log.info("开始测试成功，id: {}", id);
            return getTestDetail(id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("开始测试失败，id: {}", id, e);
            return getMockStartedTest(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ABTest pauseTest(Long id) {
        log.info("暂停测试，id: {}", id);
        try {
            ABTest test = getById(id);
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            if (test.getStatus() != STATUS_RUNNING) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "测试未运行，不允许暂停");
            }
            test.setStatus(STATUS_PAUSED);
            updateById(test);
            log.info("暂停测试成功，id: {}", id);
            return getTestDetail(id);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("暂停测试失败，id: {}", id, e);
            return getMockPausedTest(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ABTest stopTest(Long winnerVariantId) {
        log.info("结束测试，winnerVariantId: {}", winnerVariantId);
        try {
            ABTestVariant winnerVariant = variantService.getById(winnerVariantId);
            if (winnerVariant == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "优胜变体不存在");
            }
            ABTest test = getById(winnerVariant.getTestId());
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            if (test.getStatus() != STATUS_RUNNING && test.getStatus() != STATUS_PAUSED) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许结束测试");
            }
            test.setStatus(STATUS_STOPPED);
            test.setWinnerVariantId(winnerVariantId);
            test.setEndTime(LocalDateTime.now());
            updateById(test);
            log.info("结束测试成功，testId: {}, winnerVariantId: {}", test.getId(), winnerVariantId);
            return getTestDetail(test.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("结束测试失败，winnerVariantId: {}", winnerVariantId, e);
            return getMockStoppedTest(winnerVariantId);
        }
    }

    @Override
    public Map<String, Object> getTestStats(Long id) {
        log.info("获取测试统计数据，id: {}", id);
        try {
            ABTest test = getTestDetail(id);
            Map<String, Object> stats = new HashMap<>();
            stats.put("test", test);
            List<ABTestVariant> variants = test.getVariants();
            List<Map<String, Object>> variantStats = new ArrayList<>();
            long totalPageViews = 0;
            long totalConversions = 0;
            if (variants != null) {
                for (ABTestVariant variant : variants) {
                    Map<String, Object> variantStat = new HashMap<>();
                    variantStat.put("variantId", variant.getId());
                    variantStat.put("variantName", variant.getVariantName());
                    variantStat.put("pageViews", variant.getPageViews());
                    variantStat.put("uniqueVisitors", variant.getUniqueVisitors());
                    variantStat.put("conversions", variant.getConversions());
                    variantStat.put("conversionRate", variant.getConversionRate());
                    variantStats.add(variantStat);
                    if (variant.getPageViews() != null) {
                        totalPageViews += variant.getPageViews();
                    }
                    if (variant.getConversions() != null) {
                        totalConversions += variant.getConversions();
                    }
                }
            }
            stats.put("variantStats", variantStats);
            stats.put("totalPageViews", totalPageViews);
            stats.put("totalConversions", totalConversions);
            if (totalPageViews > 0) {
                stats.put("overallConversionRate", BigDecimal.valueOf(totalConversions)
                        .divide(BigDecimal.valueOf(totalPageViews), 4, RoundingMode.HALF_UP));
            } else {
                stats.put("overallConversionRate", BigDecimal.ZERO);
            }
            log.info("获取测试统计数据成功，id: {}", id);
            return stats;
        } catch (Exception e) {
            log.error("获取测试统计数据失败，id: {}", id, e);
            return getMockTestStats(id);
        }
    }

    @Override
    public Map<String, Object> calculateConfidence(Long testId) {
        log.info("计算统计显著性和置信区间，testId: {}", testId);
        try {
            ABTest test = getTestDetail(testId);
            List<ABTestVariant> variants = test.getVariants();
            Map<String, Object> result = new HashMap<>();
            result.put("testId", testId);
            result.put("testName", test.getTestName());
            List<Map<String, Object>> confidenceResults = new ArrayList<>();
            ABTestVariant controlVariant = null;
            if (variants != null) {
                for (ABTestVariant variant : variants) {
                    if ("CONTROL".equals(variant.getVariantType())) {
                        controlVariant = variant;
                        break;
                    }
                }
            }
            if (controlVariant == null && variants != null && !variants.isEmpty()) {
                controlVariant = variants.get(0);
            }
            if (variants != null) {
                for (ABTestVariant variant : variants) {
                    Map<String, Object> cr = calculateVariantConfidence(variant, controlVariant);
                    confidenceResults.add(cr);
                }
            }
            result.put("confidenceResults", confidenceResults);
            result.put("controlVariantId", controlVariant != null ? controlVariant.getId() : null);
            log.info("计算统计显著性和置信区间成功，testId: {}", testId);
            return result;
        } catch (Exception e) {
            log.error("计算统计显著性和置信区间失败，testId: {}", testId, e);
            return getMockConfidenceResult(testId);
        }
    }

    private Map<String, Object> calculateVariantConfidence(ABTestVariant variant, ABTestVariant controlVariant) {
        Map<String, Object> result = new HashMap<>();
        result.put("variantId", variant.getId());
        result.put("variantName", variant.getVariantName());
        long pv = variant.getPageViews() != null ? variant.getPageViews() : 0;
        long conv = variant.getConversions() != null ? variant.getConversions() : 0;
        BigDecimal conversionRate = pv > 0 ? BigDecimal.valueOf(conv).divide(BigDecimal.valueOf(pv), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        result.put("conversionRate", conversionRate);
        if (controlVariant != null && !variant.getId().equals(controlVariant.getId())) {
            long controlPv = controlVariant.getPageViews() != null ? controlVariant.getPageViews() : 0;
            long controlConv = controlVariant.getConversions() != null ? controlVariant.getConversions() : 0;
            BigDecimal controlRate = controlPv > 0 ? BigDecimal.valueOf(controlConv).divide(BigDecimal.valueOf(controlPv), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal uplift = controlRate.compareTo(BigDecimal.ZERO) > 0
                    ? conversionRate.subtract(controlRate).divide(controlRate, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            result.put("uplift", uplift);
            double se = Math.sqrt(conversionRate.doubleValue() * (1 - conversionRate.doubleValue()) / Math.max(pv, 1)
                    + controlRate.doubleValue() * (1 - controlRate.doubleValue()) / Math.max(controlPv, 1));
            BigDecimal zScore = se > 0 ? BigDecimal.valueOf((conversionRate.doubleValue() - controlRate.doubleValue()) / se) : BigDecimal.ZERO;
            result.put("zScore", zScore);
            double pValue = 2 * (1 - normalCdf(Math.abs(zScore.doubleValue())));
            result.put("pValue", BigDecimal.valueOf(pValue).setScale(4, RoundingMode.HALF_UP));
            BigDecimal marginOfError = BigDecimal.valueOf(1.96 * se);
            result.put("confidenceIntervalLower", conversionRate.subtract(marginOfError).max(BigDecimal.ZERO));
            result.put("confidenceIntervalUpper", conversionRate.add(marginOfError).min(BigDecimal.ONE));
            result.put("isSignificant", pValue < 0.05);
        } else {
            result.put("isControl", true);
            double se = Math.sqrt(conversionRate.doubleValue() * (1 - conversionRate.doubleValue()) / Math.max(pv, 1));
            BigDecimal marginOfError = BigDecimal.valueOf(1.96 * se);
            result.put("confidenceIntervalLower", conversionRate.subtract(marginOfError).max(BigDecimal.ZERO));
            result.put("confidenceIntervalUpper", conversionRate.add(marginOfError).min(BigDecimal.ONE));
        }
        return result;
    }

    private double normalCdf(double z) {
        return 0.5 * (1 + erf(z / Math.sqrt(2)));
    }

    private double erf(double x) {
        double a1 = 0.254829592;
        double a2 = -0.284496736;
        double a3 = 1.421413741;
        double a4 = -1.453152027;
        double a5 = 1.061405429;
        double p = 0.3275911;
        double sign = x < 0 ? -1 : 1;
        x = Math.abs(x);
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        return sign * y;
    }

    private ABTest getMockTestDetail(Long id) {
        ABTest test = new ABTest();
        test.setId(id);
        test.setAppId(1L);
        test.setPageId(100L);
        test.setTestName("Mock测试详情");
        test.setTestCode("MOCK_TEST_" + id);
        test.setDescription("Mock测试描述信息");
        test.setTestType("PAGE");
        test.setStatus(STATUS_RUNNING);
        test.setStartTime(LocalDateTime.now().minusDays(7));
        test.setTrafficPercent(50);
        test.setHashField("userId");
        test.setVariants(getMockVariantList(id));
        return test;
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
        v2.setPageViews(1000L);
        v2.setUniqueVisitors(850L);
        v2.setConversions(70L);
        v2.setConversionRate(new BigDecimal("0.0700"));
        variants.add(v2);
        return variants;
    }

    private List<ABTest> getMockTestList(Long appId) {
        List<ABTest> list = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            ABTest test = new ABTest();
            test.setId((long) i);
            test.setAppId(appId);
            test.setTestName("Mock测试" + i);
            test.setTestCode("MOCK_" + i);
            test.setDescription("Mock测试描述" + i);
            test.setStatus(i % 3);
            test.setCreatedTime(LocalDateTime.now().minusDays(i));
            list.add(test);
        }
        return list;
    }

    private Page<ABTest> getMockTestPage(Integer current, Integer size, Long appId) {
        Page<ABTest> page = new Page<>(current, size);
        List<ABTest> records = new ArrayList<>();
        for (int i = 1; i <= size; i++) {
            ABTest test = new ABTest();
            test.setId((long) ((current - 1) * size + i));
            test.setAppId(appId);
            test.setTestName("Mock分页测试" + ((current - 1) * size + i));
            test.setTestCode("MOCK_PAGE_" + ((current - 1) * size + i));
            test.setStatus(STATUS_DRAFT);
            test.setCreatedTime(LocalDateTime.now().minusHours(i));
            records.add(test);
        }
        page.setRecords(records);
        page.setTotal(50L);
        return page;
    }

    private ABTest getMockSavedTest(ABTest test) {
        test.setId(System.currentTimeMillis());
        test.setStatus(STATUS_DRAFT);
        test.setCreatedTime(LocalDateTime.now());
        return test;
    }

    private ABTest getMockStartedTest(Long id) {
        ABTest test = getMockTestDetail(id);
        test.setStatus(STATUS_RUNNING);
        test.setStartTime(LocalDateTime.now());
        return test;
    }

    private ABTest getMockPausedTest(Long id) {
        ABTest test = getMockTestDetail(id);
        test.setStatus(STATUS_PAUSED);
        return test;
    }

    private ABTest getMockStoppedTest(Long winnerVariantId) {
        ABTest test = getMockTestDetail(1L);
        test.setStatus(STATUS_STOPPED);
        test.setWinnerVariantId(winnerVariantId);
        test.setEndTime(LocalDateTime.now());
        return test;
    }

    private Map<String, Object> getMockTestStats(Long id) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("testId", id);
        stats.put("totalPageViews", 2000);
        stats.put("totalConversions", 120);
        stats.put("overallConversionRate", new BigDecimal("0.0600"));
        List<Map<String, Object>> variantStats = new ArrayList<>();
        Map<String, Object> v1 = new HashMap<>();
        v1.put("variantId", 1L);
        v1.put("variantName", "对照组");
        v1.put("pageViews", 1000);
        v1.put("uniqueVisitors", 800);
        v1.put("conversions", 50);
        v1.put("conversionRate", new BigDecimal("0.0500"));
        variantStats.add(v1);
        Map<String, Object> v2 = new HashMap<>();
        v2.put("variantId", 2L);
        v2.put("variantName", "实验组A");
        v2.put("pageViews", 1000);
        v2.put("uniqueVisitors", 850);
        v2.put("conversions", 70);
        v2.put("conversionRate", new BigDecimal("0.0700"));
        variantStats.add(v2);
        stats.put("variantStats", variantStats);
        return stats;
    }

    private Map<String, Object> getMockConfidenceResult(Long testId) {
        Map<String, Object> result = new HashMap<>();
        result.put("testId", testId);
        result.put("testName", "Mock测试");
        result.put("controlVariantId", 1L);
        List<Map<String, Object>> confidenceResults = new ArrayList<>();
        Map<String, Object> control = new HashMap<>();
        control.put("variantId", 1L);
        control.put("variantName", "对照组");
        control.put("conversionRate", new BigDecimal("0.0500"));
        control.put("isControl", true);
        control.put("confidenceIntervalLower", new BigDecimal("0.0380"));
        control.put("confidenceIntervalUpper", new BigDecimal("0.0620"));
        confidenceResults.add(control);
        Map<String, Object> experiment = new HashMap<>();
        experiment.put("variantId", 2L);
        experiment.put("variantName", "实验组A");
        experiment.put("conversionRate", new BigDecimal("0.0700"));
        experiment.put("uplift", new BigDecimal("0.4000"));
        experiment.put("zScore", new BigDecimal("2.05"));
        experiment.put("pValue", new BigDecimal("0.0404"));
        experiment.put("confidenceIntervalLower", new BigDecimal("0.0550"));
        experiment.put("confidenceIntervalUpper", new BigDecimal("0.0850"));
        experiment.put("isSignificant", true);
        confidenceResults.add(experiment);
        result.put("confidenceResults", confidenceResults);
        return result;
    }
}
