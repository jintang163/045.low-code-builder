package com.lowcode.generator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "mobile.generator")
public class MobileGeneratorConfig {

    private List<String> platforms;

    private String templatePath = "templates/uniapp";

    private String outputPath = "generated/uniapp";

    private String defaultUniAppVersion = "3.0.0";

    private String wechatAppid;

    private String alipayAppid;

    private String previewBaseUrl;

    private Integer previewExpireHours;

    private Integer previewQrCodeSize;

    private DefaultComponentConfig defaultComponents;

    @Data
    public static class DefaultComponentConfig {

        private Boolean enableTabBar = true;

        private Boolean enablePullRefresh = true;

        private Boolean enableSwiper = true;

        private Boolean enableSearchBar = true;

        private Boolean enableGrid = true;

        private Boolean enableWaterfall = false;

        private Boolean enableSwipeCell = false;

        private Boolean enableCollapse = false;
    }
}
