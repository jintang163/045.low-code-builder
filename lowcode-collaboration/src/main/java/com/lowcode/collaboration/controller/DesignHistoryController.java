package com.lowcode.collaboration.controller;

import com.lowcode.collaboration.dto.DesignHistoryCreateDTO;
import com.lowcode.collaboration.entity.DesignHistory;
import com.lowcode.collaboration.service.DesignHistoryService;
import com.lowcode.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "设计历史记录")
@RestController
@RequestMapping("/api/collaboration/history")
public class DesignHistoryController {

    @Autowired
    private DesignHistoryService historyService;

    @ApiOperation("创建设计历史记录")
    @PostMapping
    public Result<DesignHistory> create(@Validated @RequestBody DesignHistoryCreateDTO dto) {
        return Result.success(historyService.createHistory(dto));
    }

    @ApiOperation("获取目标的设计历史记录")
    @GetMapping("/list")
    public Result<List<DesignHistory>> list(@RequestParam Long appId,
                                             @RequestParam String targetType,
                                             @RequestParam Long targetId,
                                             @RequestParam(required = false, defaultValue = "100") Integer limit) {
        return Result.success(historyService.getHistoryByTarget(appId, targetType, targetId, limit));
    }
}
