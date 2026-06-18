package com.lowcode.model.controller;

import com.lowcode.common.result.Result;
import com.lowcode.generator.config.GeneratorConfig;
import com.lowcode.generator.entity.GeneratedCode;
import com.lowcode.generator.service.CodeGeneratorService;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.service.DataModelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "代码生成")
@RestController
@RequestMapping("/codeGenerator")
public class CodeGeneratorController {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private DataModelService dataModelService;

    @ApiOperation("生成全部代码")
    @PostMapping("/generateAll/{modelId}")
    public Result<List<GeneratedCode>> generateAll(@PathVariable Long modelId,
                                                   @RequestBody(required = false) GeneratorConfig config) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (config == null) {
            config = new GeneratorConfig();
        }
        return Result.success(codeGeneratorService.generateAll(model, config));
    }

    @ApiOperation("生成实体类")
    @PostMapping("/generateEntity/{modelId}")
    public Result<GeneratedCode> generateEntity(@PathVariable Long modelId,
                                                 @RequestBody(required = false) GeneratorConfig config) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (config == null) {
            config = new GeneratorConfig();
        }
        return Result.success(codeGeneratorService.generateEntity(model, config));
    }

    @ApiOperation("生成Mapper")
    @PostMapping("/generateMapper/{modelId}")
    public Result<GeneratedCode> generateMapper(@PathVariable Long modelId,
                                                 @RequestBody(required = false) GeneratorConfig config) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (config == null) {
            config = new GeneratorConfig();
        }
        return Result.success(codeGeneratorService.generateMapper(model, config));
    }

    @ApiOperation("生成Service")
    @PostMapping("/generateService/{modelId}")
    public Result<GeneratedCode> generateService(@PathVariable Long modelId,
                                                  @RequestBody(required = false) GeneratorConfig config) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (config == null) {
            config = new GeneratorConfig();
        }
        return Result.success(codeGeneratorService.generateService(model, config));
    }

    @ApiOperation("生成Controller")
    @PostMapping("/generateController/{modelId}")
    public Result<GeneratedCode> generateController(@PathVariable Long modelId,
                                                     @RequestBody(required = false) GeneratorConfig config) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (config == null) {
            config = new GeneratorConfig();
        }
        return Result.success(codeGeneratorService.generateController(model, config));
    }

    @ApiOperation("生成前端API")
    @PostMapping("/generateApiJs/{modelId}")
    public Result<GeneratedCode> generateApiJs(@PathVariable Long modelId,
                                                @RequestBody(required = false) GeneratorConfig config) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (config == null) {
            config = new GeneratorConfig();
        }
        return Result.success(codeGeneratorService.generateApiJs(model, config));
    }
}
