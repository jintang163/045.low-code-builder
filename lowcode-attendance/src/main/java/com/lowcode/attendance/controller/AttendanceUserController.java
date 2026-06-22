package com.lowcode.attendance.controller;

import com.lowcode.attendance.feign.UserFeignClient;
import com.lowcode.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "考勤员工管理")
@RestController
@RequestMapping("/attendance/user")
public class AttendanceUserController {

    @Autowired
    private UserFeignClient userFeignClient;

    @ApiOperation("获取员工列表")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getUserList(
            @RequestParam(required = false) Long appId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(required = false) String username) {
        try {
            Result<Map<String, Object>> pageResult = userFeignClient.getUserPage(pageNum, pageSize, username, 1);
            if (pageResult != null && pageResult.isSuccess() && pageResult.getData() != null) {
                List<Map<String, Object>> records = (List<Map<String, Object>>) pageResult.getData().getOrDefault("records",
                        pageResult.getData().getOrDefault("list", new ArrayList<>()));
                List<Map<String, Object>> userList = new ArrayList<>();
                for (Map<String, Object> rec : records) {
                    Map<String, Object> user = new HashMap<>();
                    user.put("id", rec.get("id"));
                    user.put("username", rec.get("username"));
                    user.put("nickname", rec.get("nickname") != null ? rec.get("nickname") : rec.get("username"));
                    user.put("avatar", rec.get("avatar"));
                    userList.add(user);
                }
                return Result.ok(userList);
            }
        } catch (Exception e) {
            return Result.ok(new ArrayList<>());
        }
        return Result.ok(new ArrayList<>());
    }
}
