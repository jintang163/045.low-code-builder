package com.lowcode.page.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.entity.ComponentLibrary;
import com.lowcode.page.service.ComponentLibraryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "组件库管理")
@RestController
@RequestMapping("/componentLibrary")
public class ComponentLibraryController {

    @Autowired
    private ComponentLibraryService componentLibraryService;

    @ApiOperation("获取组件树（按分类分组）")
    @GetMapping("/tree")
    public Result<Map<String, List<ComponentLibrary>>> getComponentTree() {
        return Result.success(componentLibraryService.getComponentTree());
    }

    @ApiOperation("获取组件列表")
    @GetMapping("/list")
    public Result<List<ComponentLibrary>> getComponentList(@RequestParam(required = false) String category) {
        return Result.success(componentLibraryService.getComponentList(category));
    }

    @ApiOperation("分页查询组件")
    @GetMapping("/page")
    public Result<Page<ComponentLibrary>> page(@RequestParam(defaultValue = "1") Integer current,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @RequestParam(required = false) String category) {
        LambdaQueryWrapper<ComponentLibrary> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq(ComponentLibrary::getComponentCategory, category);
        }
        wrapper.orderByAsc(ComponentLibrary::getId);
        Page<ComponentLibrary> page = componentLibraryService.page(new Page<>(current, size), wrapper);
        return Result.success(page);
    }

    @ApiOperation("获取组件详情")
    @GetMapping("/{id}")
    public Result<ComponentLibrary> getById(@PathVariable Long id) {
        return Result.success(componentLibraryService.getComponentDetail(id));
    }

    @ApiOperation("保存组件")
    @PostMapping
    public Result<ComponentLibrary> save(@RequestBody ComponentLibrary component) {
        return Result.success(componentLibraryService.saveComponent(component));
    }

    @ApiOperation("更新组件")
    @PutMapping
    public Result<ComponentLibrary> update(@RequestBody ComponentLibrary component) {
        return Result.success(componentLibraryService.updateComponent(component));
    }

    @ApiOperation("删除组件")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        componentLibraryService.deleteComponent(id);
        return Result.success();
    }
}
