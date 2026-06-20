package com.lowcode.generator.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MobileGenerateConfig extends AppGenerateConfig {

    private List<String> targetPlatforms;

    private String appid;

    private String wechatAppid;

    private String alipayAppid;

    private String uniAppVersion;

    private Boolean enableTouchEvents;

    private Boolean enableGestures;

    private String responsiveConfig;
}
