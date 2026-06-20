package com.lowcode.page.feign;

import com.lowcode.common.result.Result;
import com.lowcode.flow.entity.BusinessLogic;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "lowcode-flow", path = "/api/logic")
public interface BusinessLogicFeignClient {

    @GetMapping("/{id}")
    Result<BusinessLogic> getById(@PathVariable("id") Long id);

    @PutMapping
    Result<BusinessLogic> update(@RequestBody BusinessLogic logic);
}
