package com.lowcode.page.feign;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lowcode.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "lowcode-model", path = "/api/data")
public interface DataQueryFeignClient {

    @PostMapping("/list/{modelId}")
    Result<List<Map<String, Object>>> queryList(
            @PathVariable("modelId") Long modelId,
            @RequestBody(required = false) Map<String, Object> conditions,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir);

    @PostMapping("/page/{modelId}")
    Result<IPage<Map<String, Object>>> queryPage(
            @PathVariable("modelId") Long modelId,
            @RequestBody(required = false) Map<String, Object> conditions,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir);

    @GetMapping("/{modelId}/{id}")
    Result<Map<String, Object>> queryById(
            @PathVariable("modelId") Long modelId,
            @PathVariable("id") Object id);

    @PostMapping("/sql/{dataSourceId}")
    Result<List<Map<String, Object>>> executeSql(
            @PathVariable("dataSourceId") Long dataSourceId,
            @RequestBody Map<String, Object> params);

    @PostMapping("/table/{dataSourceId}/{tableName}")
    Result<List<Map<String, Object>>> queryByTable(
            @PathVariable("dataSourceId") Long dataSourceId,
            @PathVariable("tableName") String tableName,
            @RequestBody(required = false) Map<String, Object> conditions,
            @RequestParam(required = false) String orderBy,
            @RequestParam(required = false) String orderDir,
            @RequestParam(required = false) Integer limit);
}
