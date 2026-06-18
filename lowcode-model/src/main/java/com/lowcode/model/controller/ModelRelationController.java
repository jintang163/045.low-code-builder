package com.lowcode.model.controller;

import com.lowcode.common.result.Result;
import com.lowcode.model.entity.ModelRelation;
import com.lowcode.model.service.ModelRelationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "模型关联管理")
@RestController
@RequestMapping("/modelRelation")
public class ModelRelationController {

    @Autowired
    private ModelRelationService modelRelationService;

    @ApiOperation("获取应用关联列表")
    @GetMapping("/list/{appId}")
    public Result<List<ModelRelation>> listByAppId(@PathVariable Long appId) {
        return Result.success(modelRelationService.getRelationsByAppId(appId));
    }

    @ApiOperation("获取模型关联列表")
    @GetMapping("/model/{modelId}")
    public Result<List<ModelRelation>> listByModelId(@PathVariable Long modelId) {
        return Result.success(modelRelationService.getRelationsByModelId(modelId));
    }

    @ApiOperation("保存关联关系")
    @PostMapping
    public Result<ModelRelation> save(@RequestBody ModelRelation relation) {
        return Result.success(modelRelationService.saveRelation(relation));
    }

    @ApiOperation("删除关联关系")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelRelationService.deleteRelation(id);
        return Result.success();
    }
}
