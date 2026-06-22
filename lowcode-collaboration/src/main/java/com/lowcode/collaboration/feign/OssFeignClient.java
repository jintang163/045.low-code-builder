package com.lowcode.collaboration.feign;

import com.lowcode.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(name = "lowcode-oss", path = "/api/oss")
public interface OssFeignClient {

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Result<OssFileVO> upload(@RequestPart("file") MultipartFile file,
                              @RequestParam(value = "path", required = false) String path,
                              @RequestParam(value = "storageType", required = false) String storageType);
}
