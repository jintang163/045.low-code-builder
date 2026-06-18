package com.lowcode.model.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.SqlMigration;
import com.lowcode.model.service.DataModelService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "数据模型管理")
@RestController
@RequestMapping("/dataModel")
public class DataModelController {

    @Autowired
    private DataModelService dataModelService;

    @ApiOperation("保存数据模型")
    @PostMapping
    public Result<DataModel> save(@RequestBody DataModel dataModel) {
        return Result.success(dataModelService.saveModel(dataModel));
    }

    @ApiOperation("更新数据模型")
    @PutMapping
    public Result<DataModel> update(@RequestBody DataModel dataModel) {
        return Result.success(dataModelService.updateModel(dataModel));
    }

    @ApiOperation("删除数据模型")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataModelService.deleteModel(id);
        return Result.success();
    }

    @ApiOperation("获取数据模型详情")
    @GetMapping("/{id}")
    public Result<DataModel> getById(@PathVariable Long id) {
        return Result.success(dataModelService.getModelDetail(id));
    }

    @ApiOperation("获取数据模型列表")
    @GetMapping("/list/{appId}")
    public Result<List<DataModel>> list(@PathVariable Long appId) {
        return Result.success(dataModelService.getModelList(appId));
    }

    @ApiOperation("分页查询数据模型")
    @GetMapping("/page")
    public Result<Page<DataModel>> page(@RequestParam(defaultValue = "1") Integer current,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam Long appId) {
        LambdaQueryWrapper<DataModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataModel::getAppId, appId);
        wrapper.orderByDesc(DataModel::getCreatedTime);
        Page<DataModel> page = dataModelService.page(new Page<>(current, size), wrapper);
        return Result.success(page);
    }

    @ApiOperation("生成创建表SQL")
    @GetMapping("/{id}/createSql")
    public Result<String> generateCreateSql(@PathVariable Long id) {
        return Result.success(dataModelService.generateCreateSql(id));
    }

    @ApiOperation("生成删除表SQL")
    @GetMapping("/{id}/dropSql")
    public Result<String> generateDropSql(@PathVariable Long id) {
        return Result.success(dataModelService.generateDropSql(id));
    }

    @ApiOperation("发布数据模型（生成SQL迁移）")
    @PostMapping("/{id}/publish")
    public Result<SqlMigration> publish(@PathVariable Long id) {
        return Result.success(dataModelService.publishModel(id));
    }

    @ApiOperation("获取ER图数据")
    @GetMapping("/erDiagram/{appId}")
    public Result<Map<String, Object>> getErDiagram(@PathVariable Long appId) {
        return Result.success(dataModelService.getErDiagram(appId));
    }

    @ApiOperation("从数据库表导入模型")
    @PostMapping("/import")
    public Result<DataModel> importFromTable(@RequestParam Long dataSourceId,
                                             @RequestParam String tableName) {
        return Result.success(dataModelService.importFromTable(dataSourceId, tableName));
    }
}
