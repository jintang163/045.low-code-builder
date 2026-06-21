package com.lowcode.page.service.abtest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lowcode.page.entity.abtest.ABTest;

import java.util.List;
import java.util.Map;

public interface ABTestService extends IService<ABTest> {

    ABTest getTestDetail(Long id);

    List<ABTest> getTestList(Long appId, String keyword);

    Page<ABTest> getTestPage(Integer current, Integer size, Long appId, String keyword, Integer status);

    ABTest saveTest(ABTest test);

    ABTest updateTest(ABTest test);

    void deleteTest(Long id);

    ABTest startTest(Long id);

    ABTest pauseTest(Long id);

    ABTest stopTest(Long testId, Long winnerVariantId);

    Map<String, Object> getTestStats(Long id);

    Map<String, Object> calculateConfidence(Long testId);

    ABTest promoteWinner(Long testId, Long variantId);
}
