package com.lowcode.generator.controller;

import com.lowcode.common.result.Result;
import com.lowcode.generator.entity.AppGenerateConfig;
import com.lowcode.generator.entity.GeneratedApp;
import com.lowcode.generator.service.AppGeneratorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;

@Api(tags = "应用生成")
@RestController
@RequestMapping("/api/generator/app")
public class AppGeneratorController {

    @Autowired
    private AppGeneratorService appGeneratorService;

    @ApiOperation("生成应用")
    @PostMapping("/generate")
    public Result<GeneratedApp> generateApp(@RequestBody AppGenerateConfig config) throws Exception {
        return Result.success(appGeneratorService.generateApp(config));
    }

    @ApiOperation("下载应用代码包")
    @GetMapping("/download/{appCode}")
    public ResponseEntity<byte[]> downloadApp(@PathVariable String appCode) throws Exception {
        byte[] data = appGeneratorService.downloadApp(appCode);
        String fileName = URLEncoder.encode(appCode + ".zip", "UTF-8");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    @ApiOperation("预览应用生成配置")
    @PostMapping("/preview")
    public Result<AppGenerateConfig> previewConfig(@RequestBody AppGenerateConfig config) {
        return Result.success(config);
    }
}
