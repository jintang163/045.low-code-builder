package com.lowcode.model.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.entity.FieldMapping;
import com.lowcode.model.entity.VirtualView;
import com.lowcode.model.service.CrossDataSourceQueryService;
import com.lowcode.model.service.DataSourceService;
import com.lowcode.model.service.FieldMappingService;
import com.lowcode.model.service.VirtualViewService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "数据源管理")
@RestController
@RequestMapping("/api/datasource")
public class DataSourceController {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private VirtualViewService virtualViewService;

    @Autowired
    private CrossDataSourceQueryService crossDataSourceQueryService;

    @Autowired
    private FieldMappingService fieldMappingService;

    @ApiOperation("测试连接")
    @PostMapping("/testConnection")
    public Result<Boolean> testConnection(@RequestBody DataSource dataSource) {
        return Result.success(dataSourceService.testConnection(dataSource));
    }

    @ApiOperation("保存数据源")
    @PostMapping
    public Result<DataSource> save(@RequestBody DataSource dataSource) {
        return Result.success(dataSourceService.saveDataSource(dataSource));
    }

    @ApiOperation("更新数据源")
    @PutMapping
    public Result<DataSource> update(@RequestBody DataSource dataSource) {
        return Result.success(dataSourceService.updateDataSource(dataSource));
    }

    @ApiOperation("删除数据源")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dataSourceService.deleteDataSource(id);
        return Result.success();
    }

    @ApiOperation("获取数据源详情")
    @GetMapping("/{id}")
    public Result<DataSource> getById(@PathVariable Long id) {
        DataSource dataSource = dataSourceService.getById(id);
        if (dataSource != null) {
            dataSource.setPassword(null);
            dataSource.setRestApiAuthToken(null);
        }
        return Result.success(dataSource);
    }

    @ApiOperation("获取数据源列表")
    @GetMapping("/list/{appId}")
    public Result<List<DataSource>> list(@PathVariable Long appId) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getAppId, appId);
        wrapper.orderByDesc(DataSource::getCreatedTime);
        List<DataSource> list = dataSourceService.list(wrapper);
        for (DataSource ds : list) {
            ds.setPassword(null);
            ds.setRestApiAuthToken(null);
        }
        return Result.success(list);
    }

    @ApiOperation("分页查询数据源")
    @GetMapping("/page")
    public Result<Page<DataSource>> page(@RequestParam(defaultValue = "1") Integer current,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam Long appId) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getAppId, appId);
        wrapper.orderByDesc(DataSource::getCreatedTime);
        Page<DataSource> page = dataSourceService.page(new Page<>(current, size), wrapper);
        for (DataSource ds : page.getRecords()) {
            ds.setPassword(null);
            ds.setRestApiAuthToken(null);
        }
        return Result.success(page);
    }

    @ApiOperation("获取数据源表列表")
    @GetMapping("/{id}/tables")
    public Result<List<String>> getTableNames(@PathVariable Long id) {
        return Result.success(dataSourceService.getTableNames(id));
    }

    @ApiOperation("获取表字段信息")
    @GetMapping("/{id}/tables/{tableName}/columns")
    public Result<List<Map<String, Object>>> getTableColumns(@PathVariable Long id,
                                                              @PathVariable String tableName) {
        return Result.success(dataSourceService.getTableColumns(id, tableName));
    }

    @ApiOperation("获取表主键信息")
    @GetMapping("/{id}/tables/{tableName}/primaryKeys")
    public Result<List<Map<String, Object>>> getTablePrimaryKeys(@PathVariable Long id,
                                                                   @PathVariable String tableName) {
        return Result.success(dataSourceService.getTablePrimaryKeys(id, tableName));
    }

    @ApiOperation("调用REST API")
    @PostMapping("/{id}/restApi")
    public Result<List<Map<String, Object>>> callRestApi(@PathVariable Long id,
                                                          @RequestBody(required = false) Map<String, Object> params) {
        return Result.success(dataSourceService.callRestApi(id, params));
    }

    @ApiOperation("获取连接池状态")
    @GetMapping("/{id}/poolStatus")
    public Result<Map<String, Object>> getPoolStatus(@PathVariable Long id) {
        return Result.success(dataSourceService.getPoolStatus(id));
    }

    @ApiOperation("数据源健康检查")
    @PostMapping("/{id}/healthCheck")
    public Result<Map<String, Object>> healthCheck(@PathVariable Long id) {
        return Result.success(dataSourceService.healthCheck(id));
    }

    @ApiOperation("刷新连接池")
    @PostMapping("/{id}/refreshPool")
    public Result<Void> refreshPool(@PathVariable Long id) {
        dataSourceService.refreshPool(id);
        return Result.success();
    }

    @ApiOperation("保存虚拟视图")
    @PostMapping("/virtualView")
    public Result<VirtualView> saveVirtualView(@RequestBody VirtualView virtualView) {
        return Result.success(virtualViewService.saveVirtualView(virtualView));
    }

    @ApiOperation("更新虚拟视图")
    @PutMapping("/virtualView")
    public Result<VirtualView> updateVirtualView(@RequestBody VirtualView virtualView) {
        return Result.success(virtualViewService.updateVirtualView(virtualView));
    }

    @ApiOperation("删除虚拟视图")
    @DeleteMapping("/virtualView/{id}")
    public Result<Void> deleteVirtualView(@PathVariable Long id) {
        virtualViewService.deleteVirtualView(id);
        return Result.success();
    }

    @ApiOperation("获取虚拟视图列表")
    @GetMapping("/virtualView/list/{appId}")
    public Result<List<VirtualView>> listVirtualViews(@PathVariable Long appId) {
        return Result.success(virtualViewService.listByAppId(appId));
    }

    @ApiOperation("获取虚拟视图详情")
    @GetMapping("/virtualView/{id}")
    public Result<VirtualView> getVirtualView(@PathVariable Long id) {
        return Result.success(virtualViewService.getById(id));
    }

    @ApiOperation("执行虚拟视图查询")
    @PostMapping("/virtualView/{viewId}/query")
    public Result<List<Map<String, Object>>> queryVirtualView(
            @PathVariable Long viewId,
            @RequestParam(required = false, defaultValue = "1000") Integer limit) {
        return Result.success(crossDataSourceQueryService.queryVirtualView(viewId, limit));
    }

    @ApiOperation("保存字段映射")
    @PostMapping("/fieldMapping")
    public Result<FieldMapping> saveFieldMapping(@RequestBody FieldMapping fieldMapping) {
        return Result.success(fieldMappingService.saveFieldMapping(fieldMapping));
    }

    @ApiOperation("更新字段映射")
    @PutMapping("/fieldMapping")
    public Result<FieldMapping> updateFieldMapping(@RequestBody FieldMapping fieldMapping) {
        return Result.success(fieldMappingService.updateFieldMapping(fieldMapping));
    }

    @ApiOperation("删除字段映射")
    @DeleteMapping("/fieldMapping/{id}")
    public Result<Void> deleteFieldMapping(@PathVariable Long id) {
        fieldMappingService.deleteFieldMapping(id);
        return Result.success();
    }

    @ApiOperation("获取数据源字段映射列表")
    @GetMapping("/fieldMapping/dataSource/{dataSourceId}")
    public Result<List<FieldMapping>> getFieldMappingsByDataSource(@PathVariable Long dataSourceId) {
        return Result.success(fieldMappingService.listByDataSourceId(dataSourceId));
    }

    @ApiOperation("获取页面字段映射列表")
    @GetMapping("/fieldMapping/page/{pageId}")
    public Result<List<FieldMapping>> getFieldMappingsByPage(@PathVariable Long pageId) {
        return Result.success(fieldMappingService.listByPageId(pageId));
    }

    @ApiOperation("获取组件字段映射列表")
    @GetMapping("/fieldMapping/page/{pageId}/component/{componentId}")
    public Result<List<FieldMapping>> getFieldMappingsByComponent(
            @PathVariable Long pageId,
            @PathVariable String componentId) {
        return Result.success(fieldMappingService.listByComponentId(pageId, componentId));
    }

    @ApiOperation("批量保存页面字段映射")
    @PostMapping("/fieldMapping/batch/page/{pageId}")
    public Result<Boolean> saveBatchFieldMappingsByPage(
            @PathVariable Long pageId,
            @RequestBody List<FieldMapping> mappings) {
        return Result.success(fieldMappingService.saveBatchByPageId(pageId, mappings));
    }

    @ApiOperation("批量保存数据源字段映射")
    @PostMapping("/fieldMapping/batch/dataSource/{dataSourceId}/page/{pageId}")
    public Result<Boolean> saveBatchFieldMappingsByDataSource(
            @PathVariable Long dataSourceId,
            @PathVariable Long pageId,
            @RequestBody List<FieldMapping> mappings) {
        return Result.success(fieldMappingService.saveBatchByDataSourceId(dataSourceId, pageId, mappings));
    }
}
