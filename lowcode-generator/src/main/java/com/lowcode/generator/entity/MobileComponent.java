package com.lowcode.generator.entity;

import com.lowcode.page.entity.ComponentLibrary;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MobileComponent extends ComponentLibrary {

    private List<String> supportedPlatforms;

    private Boolean enableTouchEvents;

    private Boolean enableGestures;

    private String mobileDefaultProps;

    private String mobileDefaultStyle;

    private String adaptiveConfig;

    private String nativeBridgeConfig;
}
