package com.lowcode.page.controller;

import com.lowcode.common.result.Result;
import com.lowcode.page.dto.AiPageGenerateDTO;
import com.lowcode.page.service.AiPageService;
import com.lowcode.page.vo.AiPageGenerateVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "AI页面生成")
@RestController
@RequestMapping("/api/ai/page")
public class AiPageController {

    @Autowired
    private AiPageService aiPageService;

    @ApiOperation("AI生成/优化页面配置")
    @PostMapping("/generate")
    public Result<AiPageGenerateVO> generatePage(@RequestBody AiPageGenerateDTO dto) {
        return Result.success(aiPageService.generatePage(dto));
    }

    @ApiOperation("创建新的AI对话会话")
    @PostMapping("/session")
    public Result<String> createSession() {
        return Result.success(aiPageService.generateSessionId());
    }

    @ApiOperation("清除AI对话会话")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> clearSession(@PathVariable String sessionId) {
        aiPageService.clearSession(sessionId);
        return Result.success();
    }
}
