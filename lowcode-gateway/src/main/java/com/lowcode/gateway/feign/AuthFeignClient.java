package com.lowcode.gateway.feign;

import com.lowcode.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "lowcode-auth")
public interface AuthFeignClient {

    @GetMapping("/api/auth/validate")
    Result<Boolean> validateToken(@RequestParam("token") String token);
}
