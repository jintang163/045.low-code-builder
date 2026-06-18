package com.lowcode.page.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.dto.CustomComponentUploadDTO;
import com.lowcode.page.dto.CustomComponentVersionUpdateDTO;
import com.lowcode.page.service.CustomComponentService;
import com.lowcode.page.vo.CustomComponentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Api(tags = "自定义组件管理")
@RestController
@RequestMapping("/api/custom-component")
public class CustomComponentController {

    @Autowired
    private CustomComponentService customComponentService;

    @ApiOperation("上传自定义组件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CustomComponentVO> upload(@ModelAttribute CustomComponentUploadDTO dto) throws Exception {
        return Result.success(customComponentService.uploadComponent(dto));
    }

    @ApiOperation("更新组件版本")
    @PostMapping(value = "/version", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<CustomComponentVO> updateVersion(@ModelAttribute CustomComponentVersionUpdateDTO dto) throws Exception {
        return Result.success(customComponentService.updateVersion(dto));
    }

    @ApiOperation("分页查询自定义组件")
    @GetMapping("/page")
    public Result<Page<CustomComponentVO>> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return Result.success(customComponentService.page(current, size, category, keyword));
    }

    @ApiOperation("获取自定义组件树（按分类分组）")
    @GetMapping("/tree")
    public Result<Map<String, List<CustomComponentVO>>> getComponentTree() {
        return Result.success(customComponentService.getComponentTree());
    }

    @ApiOperation("获取组件详情")
    @GetMapping("/{id}")
    public Result<CustomComponentVO> getDetail(@PathVariable Long id) {
        return Result.success(customComponentService.getDetail(id));
    }

    @ApiOperation("根据组件类型获取组件信息")
    @GetMapping("/type/{componentType}")
    public Result<CustomComponentVO> getByType(
            @PathVariable String componentType,
            @RequestParam(required = false) String version) {
        return Result.success(customComponentService.getByType(componentType, version));
    }

    @ApiOperation("获取组件包下载地址")
    @GetMapping("/{id}/bundle-url")
    public Result<String> getBundleUrl(
            @PathVariable Long id,
            @RequestParam(required = false) String version) throws Exception {
        return Result.success(customComponentService.getComponentBundleUrl(id, version));
    }

    @ApiOperation("下载组件包")
    @GetMapping("/download/{componentType}")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String componentType,
            @RequestParam(required = false) String version) throws Exception {
        InputStream inputStream = customComponentService.downloadComponent(componentType, version);
        InputStreamResource resource = new InputStreamResource(inputStream);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + componentType + ".zip");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @ApiOperation("删除组件")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customComponentService.deleteComponent(id);
        return Result.success();
    }

    @ApiOperation("废弃指定版本")
    @PutMapping("/version/{versionId}/deprecate")
    public Result<Void> deprecateVersion(@PathVariable Long versionId) {
        customComponentService.deprecateVersion(versionId);
        return Result.success();
    }
}
