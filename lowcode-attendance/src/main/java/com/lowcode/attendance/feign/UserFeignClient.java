package com.lowcode.attendance.feign;

import com.lowcode.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "lowcode-auth", path = "/api")
public interface UserFeignClient {

    @GetMapping("/user/page")
    Result<Map<String, Object>> getUserPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status);

    @GetMapping("/user/{userId}/roles")
    Result<List<String>> getUserRoles(@PathVariable("userId") Long userId);
}
