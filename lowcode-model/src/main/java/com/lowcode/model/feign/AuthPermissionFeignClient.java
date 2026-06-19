package com.lowcode.model.feign;

import com.lowcode.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lowcode-auth", path = "/api/permission")
public interface AuthPermissionFeignClient {

    @GetMapping("/row/filter")
    Result<String> getRowLevelFilter(
            @RequestParam("appId") Long appId,
            @RequestParam("modelId") Long modelId
    );

    @GetMapping("/check/field")
    Result<Boolean> checkFieldPermission(
            @RequestParam("appId") Long appId,
            @RequestParam("modelId") Long modelId,
            @RequestParam("fieldId") Long fieldId,
            @RequestParam("action") String action
    );
}
