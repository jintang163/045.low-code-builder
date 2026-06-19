package com.lowcode.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lowcode.auth.dto.RowPermissionDTO;
import com.lowcode.auth.entity.SysComponentPermission;
import com.lowcode.auth.entity.SysFieldPermission;
import com.lowcode.auth.entity.SysPagePermission;
import com.lowcode.auth.entity.SysRowPermission;
import com.lowcode.auth.service.PermissionService;
import com.lowcode.auth.util.RowPermissionExpressionEngine;
import com.lowcode.auth.vo.UserPermissionVO;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.UserContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "权限管理")
@RestController
@RequestMapping("/api/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @ApiOperation("获取当前用户权限")
    @GetMapping("/current")
    public Result<UserPermissionVO> getCurrentUserPermissions(@RequestParam Long appId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(permissionService.getUserPermissions(userId, appId));
    }

    @ApiOperation("获取用户权限")
    @GetMapping("/user/{userId}")
    public Result<UserPermissionVO> getUserPermissions(@PathVariable Long userId, @RequestParam Long appId) {
        return Result.success(permissionService.getUserPermissions(userId, appId));
    }

    @ApiOperation("检查页面访问权限")
    @GetMapping("/check/page")
    public Result<Boolean> checkPagePermission(
            @RequestParam Long appId,
            @RequestParam Long pageId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(permissionService.checkPagePermission(userId, appId, pageId));
    }

    @ApiOperation("批量检查组件权限")
    @PostMapping("/check/components")
    public Result<Map<String, Boolean>> checkComponentPermissions(
            @RequestParam Long appId,
            @RequestParam Long pageId,
            @RequestBody List<String> componentIds) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(permissionService.checkComponentPermissions(userId, appId, pageId, componentIds));
    }

    @ApiOperation("检查字段权限")
    @GetMapping("/check/field")
    public Result<Boolean> checkFieldPermission(
            @RequestParam Long appId,
            @RequestParam Long modelId,
            @RequestParam Long fieldId,
            @RequestParam String action) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(permissionService.checkFieldPermission(userId, appId, modelId, fieldId, action));
    }

    @ApiOperation("获取字段权限列表")
    @GetMapping("/fields")
    public Result<Map<Long, UserPermissionVO.FieldPermissionVO>> getFieldPermissions(
            @RequestParam Long appId,
            @RequestParam Long modelId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(permissionService.getFieldPermissions(userId, appId, modelId));
    }

    @ApiOperation("获取行级权限SQL过滤条件")
    @GetMapping("/row/filter")
    public Result<String> getRowLevelFilter(
            @RequestParam Long appId,
            @RequestParam Long modelId) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(permissionService.getRowLevelFilter(userId, appId, modelId));
    }

    @ApiOperation("应用行级权限过滤数据")
    @PostMapping("/row/filterData")
    public Result<List<Map<String, Object>>> applyRowLevelFilter(
            @RequestParam Long appId,
            @RequestParam Long modelId,
            @RequestBody List<Map<String, Object>> dataList) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(permissionService.applyRowLevelFilter(dataList, userId, appId, modelId));
    }

    @ApiOperation("创建行级权限")
    @PostMapping("/row")
    public Result<Long> createRowPermission(@RequestBody RowPermissionDTO dto) {
        return Result.success(permissionService.createRowPermission(dto));
    }

    @ApiOperation("更新行级权限")
    @PutMapping("/row/{id}")
    public Result<Void> updateRowPermission(@PathVariable Long id, @RequestBody RowPermissionDTO dto) {
        permissionService.updateRowPermission(id, dto);
        return Result.success();
    }

    @ApiOperation("删除行级权限")
    @DeleteMapping("/row/{id}")
    public Result<Void> deleteRowPermission(@PathVariable Long id) {
        permissionService.deleteRowPermission(id);
        return Result.success();
    }

    @ApiOperation("获取行级权限列表")
    @GetMapping("/row/list")
    public Result<List<SysRowPermission>> getRowPermissions(
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long modelId) {
        return Result.success(permissionService.getRowPermissions(appId, roleId, modelId));
    }

    @ApiOperation("验证权限表达式")
    @PostMapping("/expression/validate")
    public Result<RowPermissionExpressionEngine.ExpressionParseResult> validateExpression(
            @RequestBody Map<String, String> params) {
        String expression = params.get("expression");
        return Result.success(permissionService.validateExpression(expression));
    }

    @ApiOperation("执行权限表达式")
    @PostMapping("/expression/evaluate")
    public Result<Boolean> evaluateExpression(
            @RequestBody Map<String, Object> params) {
        String expression = (String) params.get("expression");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) params.get("data");
        return Result.success(permissionService.evaluateExpression(expression, data));
    }

    @ApiOperation("分配用户应用角色")
    @PostMapping("/user/roles")
    public Result<Void> assignUserAppRoles(
            @RequestParam Long userId,
            @RequestParam Long appId,
            @RequestBody List<Long> roleIds) {
        permissionService.assignUserAppRoles(userId, appId, roleIds);
        return Result.success();
    }

    @ApiOperation("保存字段权限配置")
    @PostMapping("/fields/batch")
    public Result<Void> saveFieldPermissions(
            @RequestParam Long roleId,
            @RequestParam Long modelId,
            @RequestBody List<SysFieldPermission> permissions) {
        permissionService.saveFieldPermissions(roleId, modelId, permissions);
        return Result.success();
    }

    @ApiOperation("保存页面权限配置")
    @PostMapping("/pages/batch")
    public Result<Void> savePagePermissions(
            @RequestParam Long roleId,
            @RequestBody List<SysPagePermission> permissions) {
        permissionService.savePagePermissions(roleId, permissions);
        return Result.success();
    }

    @ApiOperation("保存组件权限配置")
    @PostMapping("/components/batch")
    public Result<Void> saveComponentPermissions(
            @RequestParam Long roleId,
            @RequestParam Long pageId,
            @RequestBody List<SysComponentPermission> permissions) {
        permissionService.saveComponentPermissions(roleId, pageId, permissions);
        return Result.success();
    }
}
