package com.lowcode.generator.config;

import lombok.Data;

@Data
public class GeneratorConfig {

    private String basePackage = "com.lowcode.generated";
    private String moduleName;
    private String entityPackage = "entity";
    private String mapperPackage = "mapper";
    private String servicePackage = "service";
    private String controllerPackage = "controller";
    private String voPackage = "vo";
    private String dtoPackage = "dto";

    private String tablePrefix;
    private Boolean restController = true;
    private Boolean swaggerSupport = true;
    private Boolean lombokSupport = true;
    private Boolean mybatisPlusSupport = true;

    private String author = "lowcode-platform";
    private String version = "1.0.0";

    private Boolean generateEntity = true;
    private Boolean generateMapper = true;
    private Boolean generateService = true;
    private Boolean generateController = true;
    private Boolean generateVo = true;
    private Boolean generateDto = true;
    private Boolean generateApiJs = true;
}
