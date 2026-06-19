package com.lowcode.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lowcode.auth.entity.SysRole;
import com.lowcode.auth.service.SysRoleService;
import com.lowcode.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "角色管理")
@RestController
@RequestMapping("/api/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @ApiOperation("分页查询角色列表")
    @GetMapping("/page")
    public Result<IPage<SysRole>> getRolePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) String roleName) {
        return Result.success(sysRoleService.getRolePage(pageNum, pageSize, appId, roleName));
    }

    @ApiOperation("查询角色列表")
    @GetMapping("/list")
    public Result<List<SysRole>> getRoles(@RequestParam(required = false) Long appId) {
        return Result.success(sysRoleService.getRolesByAppId(appId));
    }

    @ApiOperation("根据ID查询角色")
    @GetMapping("/{id}")
    public Result<SysRole> getRoleById(@PathVariable Long id) {
        return Result.success(sysRoleService.getRoleById(id));
    }

    @ApiOperation("创建角色")
    @PostMapping
    public Result<Long> createRole(@RequestBody SysRole role) {
        return Result.success(sysRoleService.createRole(role));
    }

    @ApiOperation("更新角色")
    @PutMapping("/{id}")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody SysRole role) {
        sysRoleService.updateRole(id, role);
        return Result.success();
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return Result.success();
    }
}
