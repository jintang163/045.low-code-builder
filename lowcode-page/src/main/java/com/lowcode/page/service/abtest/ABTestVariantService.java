package com.lowcode.page.service.abtest;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lowcode.page.entity.abtest.ABTestVariant;

import java.util.List;

public interface ABTestVariantService extends IService<ABTestVariant> {

    List<ABTestVariant> getVariantList(Long testId);

    ABTestVariant saveVariant(ABTestVariant variant);

    ABTestVariant updateVariant(ABTestVariant variant);

    void deleteVariant(Long id);

    void incrementPageView(Long variantId, Long userId);

    void incrementConversion(Long variantId, Long userId, String eventKey);
}
