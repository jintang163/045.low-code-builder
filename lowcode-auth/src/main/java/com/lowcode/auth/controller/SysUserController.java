package com.lowcode.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lowcode.auth.entity.SysUser;
import com.lowcode.auth.service.SysUserService;
import com.lowcode.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @ApiOperation("分页查询用户列表")
    @GetMapping("/page")
    public Result<IPage<SysUser>> getUserPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        return Result.success(sysUserService.getUserPage(pageNum, pageSize, username, status));
    }

    @ApiOperation("根据ID查询用户")
    @GetMapping("/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        return Result.success(sysUserService.getUserById(id));
    }

    @ApiOperation("创建用户")
    @PostMapping
    public Result<Void> createUser(@RequestBody Map<String, Object> params) {
        SysUser user = com.alibaba.fastjson2.JSONObject.parseObject(
                com.alibaba.fastjson2.JSONObject.toJSONString(params.get("user")), SysUser.class);
        @SuppressWarnings("unchecked")
        List<Long> roleIds = (List<Long>) params.get("roleIds");
        sysUserService.createUser(user, roleIds);
        return Result.success();
    }

    @ApiOperation("更新用户")
    @PutMapping
    public Result<Void> updateUser(@RequestBody Map<String, Object> params) {
        SysUser user = com.alibaba.fastjson2.JSONObject.parseObject(
                com.alibaba.fastjson2.JSONObject.toJSONString(params.get("user")), SysUser.class);
        @SuppressWarnings("unchecked")
        List<Long> roleIds = (List<Long>) params.get("roleIds");
        sysUserService.updateUser(user, roleIds);
        return Result.success();
    }

    @ApiOperation("删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }

    @ApiOperation("重置密码")
    @PutMapping("/resetPassword")
    public Result<Void> resetPassword(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        String newPassword = params.get("newPassword").toString();
        sysUserService.resetPassword(id, newPassword);
        return Result.success();
    }

    @ApiOperation("获取用户角色")
    @GetMapping("/{userId}/roles")
    public Result<List<String>> getUserRoles(@PathVariable Long userId) {
        return Result.success(sysUserService.getUserRoles(userId));
    }

    @ApiOperation("获取用户权限")
    @GetMapping("/{userId}/permissions")
    public Result<List<String>> getUserPermissions(@PathVariable Long userId) {
        return Result.success(sysUserService.getUserPermissions(userId));
    }
}
