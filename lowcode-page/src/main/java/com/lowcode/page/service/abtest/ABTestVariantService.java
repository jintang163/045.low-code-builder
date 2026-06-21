package com.lowcode.page.service.abtest;

import com.lowcode.page.entity.abtest.ABTestVariant;

import java.util.List;

public interface ABTestVariantService {

    List<ABTestVariant> getVariantList(Long testId);

    ABTestVariant saveVariant(ABTestVariant variant);

    ABTestVariant updateVariant(ABTestVariant variant);

    void deleteVariant(Long id);

    void incrementPageView(Long variantId, String userId);

    void incrementConversion(Long variantId, String userId, String eventKey);
}
