package com.lowcode.generator.controller;

import com.lowcode.common.result.Result;
import com.lowcode.generator.entity.GeneratedApp;
import com.lowcode.generator.entity.MobileGenerateConfig;
import com.lowcode.generator.entity.MobilePreview;
import com.lowcode.generator.entity.MobilePreviewRequest;
import com.lowcode.generator.service.AppGeneratorService;
import com.lowcode.generator.service.MobilePreviewService;
import com.lowcode.generator.service.MobilePreviewService.DeviceConfig;
import com.lowcode.generator.service.MobilePreviewService.SimulatorConfig;
import com.lowcode.generator.service.UniAppCodeGeneratorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.net.URLEncoder;
import java.util.List;

@Slf4j
@Api(tags = "uni-app 生成与预览")
@RestController
@RequestMapping("/api/uniapp")
@Validated
public class UniAppGeneratorController {

    @Autowired
    private UniAppCodeGeneratorService uniAppCodeGeneratorService;

    @Autowired
    private MobilePreviewService mobilePreviewService;

    @Autowired
    private AppGeneratorService appGeneratorService;

    @ApiOperation("生成 uni-app 项目代码")
    @PostMapping("/generate")
    public Result<GeneratedApp> generateUniApp(@Valid @RequestBody MobileGenerateConfig config) {
        try {
            GeneratedApp app = uniAppCodeGeneratorService.generateUniApp(config);
            return Result.success(app);
        } catch (Exception e) {
            log.error("生成 uni-app 项目代码失败", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }

    @ApiOperation("创建移动端预览")
    @PostMapping("/preview")
    public Result<MobilePreview> createPreview(@Valid @RequestBody MobilePreviewRequest request) {
        try {
            MobilePreview preview = mobilePreviewService.createPreview(
                    request.getAppId(),
                    request.getPageId(),
                    request.getPlatform()
            );
            return Result.success(preview);
        } catch (Exception e) {
            log.error("创建移动端预览失败", e);
            return Result.error("创建预览失败: " + e.getMessage());
        }
    }

    @ApiOperation("获取预览信息")
    @GetMapping("/preview/{previewToken}")
    public Result<MobilePreview> getPreview(
            @ApiParam("预览令牌") @PathVariable @NotBlank(message = "预览令牌不能为空") String previewToken) {
        try {
            MobilePreview preview = mobilePreviewService.getPreview(previewToken);
            if (preview == null) {
                return Result.error("预览不存在或已过期");
            }
            return Result.success(preview);
        } catch (Exception e) {
            log.error("获取预览信息失败", e);
            return Result.error("获取预览信息失败: " + e.getMessage());
        }
    }

    @ApiOperation("获取模拟器配置")
    @GetMapping("/preview/{previewToken}/simulator")
    public Result<SimulatorConfig> getSimulatorConfig(
            @ApiParam("预览令牌") @PathVariable @NotBlank(message = "预览令牌不能为空") String previewToken,
            @ApiParam("设备类型") @RequestParam(defaultValue = "iPhone14") String deviceType) {
        try {
            SimulatorConfig config = mobilePreviewService.getSimulatorConfig(previewToken, deviceType);
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取模拟器配置失败", e);
            return Result.error("获取模拟器配置失败: " + e.getMessage());
        }
    }

    @ApiOperation("获取支持的设备列表")
    @GetMapping("/devices")
    public Result<List<DeviceConfig>> getDevices() {
        try {
            List<DeviceConfig> devices = mobilePreviewService.getSupportedDevices();
            return Result.success(devices);
        } catch (Exception e) {
            log.error("获取设备列表失败", e);
            return Result.error("获取设备列表失败: " + e.getMessage());
        }
    }

    @ApiOperation("获取二维码图片")
    @GetMapping(value = "/preview/{previewToken}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(
            @ApiParam("预览令牌") @PathVariable @NotBlank(message = "预览令牌不能为空") String previewToken,
            @ApiParam("二维码尺寸") @RequestParam(defaultValue = "256") Integer size) {
        try {
            if (size < 100 || size > 1024) {
                return ResponseEntity.badRequest().body(null);
            }
            byte[] qrCode = mobilePreviewService.getQrCode(previewToken, size);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(qrCode);
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @ApiOperation("获取支持的目标平台列表")
    @GetMapping("/platforms")
    public Result<List<String>> getPlatforms() {
        try {
            List<String> platforms = uniAppCodeGeneratorService.getSupportedPlatforms();
            return Result.success(platforms);
        } catch (Exception e) {
            log.error("获取平台列表失败", e);
            return Result.error("获取平台列表失败: " + e.getMessage());
        }
    }

    @ApiOperation("手动过期预览")
    @PostMapping("/preview/{previewToken}/expire")
    public Result<Void> expirePreview(
            @ApiParam("预览令牌") @PathVariable @NotBlank(message = "预览令牌不能为空") String previewToken) {
        try {
            mobilePreviewService.expirePreview(previewToken);
            return Result.success();
        } catch (Exception e) {
            log.error("过期预览失败", e);
            return Result.error("过期预览失败: " + e.getMessage());
        }
    }

    @ApiOperation("下载生成的 uni-app 项目 zip 包")
    @GetMapping("/download/{appCode}")
    public ResponseEntity<byte[]> downloadUniApp(
            @ApiParam("应用编码") @PathVariable @NotBlank(message = "应用编码不能为空") String appCode) {
        try {
            byte[] data = uniAppCodeGeneratorService.downloadUniApp(appCode);
            if (data == null || data.length == 0) {
                return ResponseEntity.notFound().build();
            }
            String fileName = URLEncoder.encode(appCode + "-uniapp.zip", "UTF-8");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(data);
        } catch (Exception e) {
            log.error("下载 uni-app 项目失败", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
