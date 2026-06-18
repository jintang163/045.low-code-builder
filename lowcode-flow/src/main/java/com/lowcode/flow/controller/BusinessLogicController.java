package com.lowcode.flow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.flow.entity.BusinessLogic;
import com.lowcode.flow.service.BusinessLogicService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "业务逻辑编排")
@RestController
@RequestMapping("/businessLogic")
public class BusinessLogicController {

    @Autowired
    private BusinessLogicService businessLogicService;

    @ApiOperation("保存业务逻辑")
    @PostMapping
    public Result<BusinessLogic> save(@RequestBody BusinessLogic logic) {
        return Result.success(businessLogicService.saveLogic(logic));
    }

    @ApiOperation("更新业务逻辑")
    @PutMapping
    public Result<BusinessLogic> update(@RequestBody BusinessLogic logic) {
        return Result.success(businessLogicService.updateLogic(logic));
    }

    @ApiOperation("删除业务逻辑")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        businessLogicService.deleteLogic(id);
        return Result.success();
    }

    @ApiOperation("获取业务逻辑详情")
    @GetMapping("/{id}")
    public Result<BusinessLogic> getById(@PathVariable Long id) {
        return Result.success(businessLogicService.getLogicDetail(id));
    }

    @ApiOperation("获取业务逻辑列表")
    @GetMapping("/list/{appId}")
    public Result<List<BusinessLogic>> list(@PathVariable Long appId) {
        return Result.success(businessLogicService.getLogicList(appId));
    }

    @ApiOperation("分页查询业务逻辑")
    @GetMapping("/page")
    public Result<Page<BusinessLogic>> page(@RequestParam(defaultValue = "1") Integer current,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam Long appId) {
        LambdaQueryWrapper<BusinessLogic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BusinessLogic::getAppId, appId);
        wrapper.orderByDesc(BusinessLogic::getCreatedTime);
        Page<BusinessLogic> page = businessLogicService.page(new Page<>(current, size), wrapper);
        return Result.success(page);
    }

    @ApiOperation("生成业务逻辑代码")
    @GetMapping("/{id}/generateCode")
    public Result<String> generateCode(@PathVariable Long id) {
        return Result.success(businessLogicService.generateLogicCode(id));
    }

    @ApiOperation("发布业务逻辑")
    @PostMapping("/{id}/publish")
    public Result<BusinessLogic> publish(@PathVariable Long id) {
        return Result.success(businessLogicService.publishLogic(id));
    }

    @ApiOperation("获取节点类型列表")
    @GetMapping("/nodeTypes")
    public Result<Map<String, Object>> getNodeTypes() {
        return Result.success(businessLogicService.getNodeTypes());
    }
}
