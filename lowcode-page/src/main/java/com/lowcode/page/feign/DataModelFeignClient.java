package com.lowcode.page.feign;

import com.lowcode.common.result.Result;
import com.lowcode.model.entity.DataModel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "lowcode-model", path = "/api/model")
public interface DataModelFeignClient {

    @GetMapping("/{id}")
    Result<DataModel> getById(@PathVariable("id") Long id);

    @PutMapping
    Result<DataModel> update(@RequestBody DataModel dataModel);
}
