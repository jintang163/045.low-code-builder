package com.lowcode.oss.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lowcode.common.result.Result;
import com.lowcode.oss.entity.OssFile;
import com.lowcode.oss.service.OssService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Api(tags = "文件存储")
@RestController
@RequestMapping("/api/oss")
public class OssController {

    @Autowired
    private OssService ossService;

    @ApiOperation("分页查询文件列表")
    @GetMapping("/page")
    public Result<IPage<OssFile>> getFilePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String storageType) {
        return Result.success(ossService.getFilePage(pageNum, pageSize, fileName, storageType));
    }

    @ApiOperation("根据ID查询文件")
    @GetMapping("/{id}")
    public Result<OssFile> getFileById(@PathVariable Long id) {
        return Result.success(ossService.getFileById(id));
    }

    @ApiOperation("上传文件")
    @PostMapping("/upload")
    public Result<OssFile> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String storageType,
            HttpServletRequest request) throws Exception {
        return Result.success(ossService.upload(file, path, storageType, request));
    }

    @ApiOperation("下载文件")
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) throws Exception {
        ossService.download(id, response);
    }

    @ApiOperation("删除文件")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) throws Exception {
        ossService.delete(id);
        return Result.success();
    }

    @ApiOperation("获取文件访问URL")
    @GetMapping("/url/{id}")
    public Result<String> getFileUrl(@PathVariable Long id) {
        return Result.success(ossService.getFileUrl(id));
    }
}
