package com.lowcode.flow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.model.Result;
import com.lowcode.flow.dto.CrossAppCallDTO;
import com.lowcode.flow.entity.AppExposedApi;
import com.lowcode.flow.entity.AppExposedEvent;
import com.lowcode.flow.service.AppExposedEventService;
import com.lowcode.flow.service.CrossAppService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cross-app")
@Api(tags = "跨应用服务调用")
public class CrossAppController {

    @Autowired
    private CrossAppService crossAppService;

    @Autowired
    private AppExposedEventService appExposedEventService;

    @PostMapping("/call")
    @ApiOperation("执行跨应用调用")
    public Result<Map<String, Object>> executeCall(@RequestBody CrossAppCallDTO dto) {
        Map<String, Object> result = crossAppService.executeCrossAppCall(dto);
        return Result.success(result);
    }

    @PostMapping("/event/publish")
    @ApiOperation("发布事件")
    public Result<Map<String, Object>> publishEvent(@RequestBody Map<String, Object> payload) {
        log.info("接收到事件发布: {}", payload);
        Map<String, Object> result = new HashMap<>();
        result.put("eventCode", payload.get("eventCode"));
        result.put("published", true);
        result.put("eventId", "evt_" + System.currentTimeMillis());
        return Result.success(result);
    }

    @PostMapping("/api/register")
    @ApiOperation("注册应用暴露的API接口")
    public Result<AppExposedApi> registerApi(@RequestBody AppExposedApi api) {
        AppExposedApi result = crossAppService.registerApi(api);
        return Result.success(result);
    }

    @PutMapping("/api")
    @ApiOperation("更新API接口")
    public Result<AppExposedApi> updateApi(@RequestBody AppExposedApi api) {
        AppExposedApi result = crossAppService.updateApi(api);
        return Result.success(result);
    }

    @DeleteMapping("/api/{id}")
    @ApiOperation("删除API接口")
    public Result<Void> deleteApi(@PathVariable Long id) {
        crossAppService.deleteApi(id);
        return Result.success();
    }

    @GetMapping("/api/{id}")
    @ApiOperation("获取API详情")
    public Result<AppExposedApi> getApi(@PathVariable Long id) {
        return Result.success(crossAppService.getApiById(id));
    }

    @GetMapping("/api/code/{code}")
    @ApiOperation("根据编码获取API详情")
    public Result<AppExposedApi> getApiByCode(@PathVariable String code) {
        return Result.success(crossAppService.getByCode(code));
    }

    @GetMapping("/api/list/app/{appId}")
    @ApiOperation("获取应用的API列表")
    public Result<List<AppExposedApi>> listApisByApp(@PathVariable Long appId) {
        return Result.success(crossAppService.listApisByApp(appId));
    }

    @GetMapping("/api/list/app-code/{appCode}")
    @ApiOperation("根据应用编码获取启用的API列表")
    public Result<List<AppExposedApi>> listApisByAppCode(@PathVariable String appCode) {
        return Result.success(crossAppService.listApisByAppCode(appCode));
    }

    @GetMapping("/api/page")
    @ApiOperation("分页查询API列表")
    public Result<Page<AppExposedApi>> pageApis(
            @ApiParam(value = "当前页", defaultValue = "1") @RequestParam(defaultValue = "1") Integer current,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam(value = "应用ID") @RequestParam(required = false) Long appId,
            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword) {
        return Result.success(crossAppService.pageApis(current, size, appId, keyword));
    }

    @PostMapping("/event/register")
    @ApiOperation("注册应用暴露的事件")
    public Result<AppExposedEvent> registerEvent(@RequestBody AppExposedEvent event) {
        AppExposedEvent result = crossAppService.registerEvent(event);
        return Result.success(result);
    }

    @PutMapping("/event")
    @ApiOperation("更新事件")
    public Result<AppExposedEvent> updateEvent(@RequestBody AppExposedEvent event) {
        AppExposedEvent result = appExposedEventService.saveEvent(event);
        return Result.success(result);
    }

    @DeleteMapping("/event/{id}")
    @ApiOperation("删除事件")
    public Result<Void> deleteEvent(@PathVariable Long id) {
        appExposedEventService.deleteEvent(id);
        return Result.success();
    }

    @GetMapping("/event/{id}")
    @ApiOperation("获取事件详情")
    public Result<AppExposedEvent> getEvent(@PathVariable Long id) {
        return Result.success(appExposedEventService.getById(id));
    }

    @GetMapping("/event/code/{code}")
    @ApiOperation("根据编码获取事件详情")
    public Result<AppExposedEvent> getEventByCode(@PathVariable String code) {
        return Result.success(appExposedEventService.getByCode(code));
    }

    @GetMapping("/event/list/app/{appId}")
    @ApiOperation("获取应用的事件列表")
    public Result<List<AppExposedEvent>> listEventsByApp(@PathVariable Long appId) {
        return Result.success(appExposedEventService.listByApp(appId));
    }

    @GetMapping("/event/list/app-code/{appCode}")
    @ApiOperation("根据应用编码获取启用的事件列表")
    public Result<List<AppExposedEvent>> listEventsByAppCode(@PathVariable String appCode) {
        return Result.success(crossAppService.listEventsByAppCode(appCode));
    }

    @GetMapping("/event/page")
    @ApiOperation("分页查询事件列表")
    public Result<Page<AppExposedEvent>> pageEvents(
            @ApiParam(value = "当前页", defaultValue = "1") @RequestParam(defaultValue = "1") Integer current,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam(value = "应用ID") @RequestParam(required = false) Long appId,
            @ApiParam(value = "关键词") @RequestParam(required = false) String keyword) {
        return Result.success(appExposedEventService.pageEvents(current, size, appId, keyword));
    }

    @GetMapping("/discover")
    @ApiOperation("发现所有可用的应用服务")
    public Result<Map<String, Object>> discoverServices(
            @ApiParam(value = "应用编码，可选") @RequestParam(required = false) String appCode) {
        Map<String, Object> result = new HashMap<>();
        if (appCode != null && !appCode.trim().isEmpty()) {
            result.put("apis", crossAppService.listApisByAppCode(appCode));
            result.put("events", crossAppService.listEventsByAppCode(appCode));
        } else {
            result.put("apis", crossAppService.list());
            result.put("events", appExposedEventService.list());
        }
        return Result.success(result);
    }
}
