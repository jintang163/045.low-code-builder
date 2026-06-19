package com.lowcode.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.auth.dto.RowPermissionDTO;
import com.lowcode.auth.entity.*;
import com.lowcode.auth.mapper.*;
import com.lowcode.auth.util.RowPermissionExpressionEngine;
import com.lowcode.auth.vo.UserPermissionVO;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.util.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PermissionService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysPermissionMapper sysPermissionMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRolePermissionMapper sysRolePermissionMapper;

    @Autowired
    private SysAppRoleMapper sysAppRoleMapper;

    @Autowired
    private SysUserAppRoleMapper sysUserAppRoleMapper;

    @Autowired
    private SysPagePermissionMapper sysPagePermissionMapper;

    @Autowired
    private SysComponentPermissionMapper sysComponentPermissionMapper;

    @Autowired
    private SysFieldPermissionMapper sysFieldPermissionMapper;

    @Autowired
    private SysRowPermissionMapper sysRowPermissionMapper;

    @Autowired
    private RowPermissionExpressionEngine expressionEngine;

    public UserPermissionVO getUserPermissions(Long userId, Long appId) {
        UserPermissionVO vo = new UserPermissionVO();
        vo.setUserId(userId);

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        vo.setUsername(user.getUsername());

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(userId);
        List<SysAppRole> appRoles = sysAppRoleMapper.selectUserAppRoles(userId, appId);
        List<String> appRoleCodes = appRoles.stream()
                .map(SysAppRole::getRoleCode)
                .collect(Collectors.toList());
        roles.addAll(appRoleCodes);
        vo.setRoles(roles);

        List<String> permissions = sysUserMapper.selectPermissionCodesByUserId(userId);
        vo.setPermissions(permissions);

        List<Long> roleIds = sysUserAppRoleMapper.selectRoleIdsByUserAndApp(userId, appId);
        if (roleIds.isEmpty()) {
            roleIds = sysUserRoleMapper.selectObjs(
                new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getUserId, userId)
                    .select(SysUserRole::getRoleId)
            );
        }

        if (!roleIds.isEmpty()) {
            List<SysPagePermission> pagePermissions = sysPagePermissionMapper.selectBatchIds(
                sysPagePermissionMapper.selectObjs(
                    new LambdaQueryWrapper<SysPagePermission>()
                        .in(SysPagePermission::getRoleId, roleIds)
                        .eq(SysPagePermission::getCanAccess, 1)
                        .select(SysPagePermission::getId)
                )
            );
            List<Long> pageIds = pagePermissions.stream()
                    .map(SysPagePermission::getPageId)
                    .collect(Collectors.toList());
            vo.setAccessiblePageIds(pageIds);

            List<SysComponentPermission> compPermissions = sysComponentPermissionMapper.selectBatchIds(
                sysComponentPermissionMapper.selectObjs(
                    new LambdaQueryWrapper<SysComponentPermission>()
                        .in(SysComponentPermission::getRoleId, roleIds)
                        .select(SysComponentPermission::getId)
                )
            );
            Map<String, UserPermissionVO.ComponentPermissionVO> compPermMap = new HashMap<>();
            for (SysComponentPermission cp : compPermissions) {
                UserPermissionVO.ComponentPermissionVO cpVO = new UserPermissionVO.ComponentPermissionVO();
                cpVO.setComponentId(cp.getComponentId());
                cpVO.setVisible(cp.getCanVisible() == 1);
                cpVO.setDisabled(cp.getCanDisabled() == 1);
                compPermMap.put(cp.getComponentId(), cpVO);
            }
            vo.setComponentPermissions(compPermMap);
        }

        return vo;
    }

    public Map<String, Boolean> checkComponentPermissions(Long userId, Long appId, Long pageId, List<String> componentIds) {
        Map<String, Boolean> result = new HashMap<>();
        for (String componentId : componentIds) {
            result.put(componentId, true);
        }

        List<SysComponentPermission> permissions = sysComponentPermissionMapper.selectComponentPermissions(userId, appId, pageId);
        for (SysComponentPermission perm : permissions) {
            if (perm.getCanVisible() == 0) {
                result.put(perm.getComponentId(), false);
            }
        }

        return result;
    }

    public String getRowLevelFilter(Long userId, Long appId, Long modelId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        List<SysRowPermission> permissions = sysRowPermissionMapper.selectRowPermissions(userId, appId, modelId);
        return expressionEngine.generateSqlFilter(permissions, user);
    }

    public List<Map<String, Object>> applyRowLevelFilter(List<Map<String, Object>> dataList,
                                                         Long userId, Long appId, Long modelId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return dataList;
        }

        List<SysRowPermission> permissions = sysRowPermissionMapper.selectRowPermissions(userId, appId, modelId);
        return expressionEngine.filterData(dataList, permissions, user);
    }

    public boolean checkFieldPermission(Long userId, Long appId, Long modelId, Long fieldId, String action) {
        List<SysFieldPermission> permissions = sysFieldPermissionMapper.selectFieldPermissions(userId, appId, modelId);
        for (SysFieldPermission perm : permissions) {
            if (perm.getFieldId().equals(fieldId)) {
                if ("view".equalsIgnoreCase(action)) {
                    return perm.getCanView() == 1;
                } else if ("edit".equalsIgnoreCase(action)) {
                    return perm.getCanEdit() == 1;
                }
            }
        }
        return true;
    }

    public Map<Long, UserPermissionVO.FieldPermissionVO> getFieldPermissions(Long userId, Long appId, Long modelId) {
        List<SysFieldPermission> permissions = sysFieldPermissionMapper.selectFieldPermissions(userId, appId, modelId);
        Map<Long, UserPermissionVO.FieldPermissionVO> result = new HashMap<>();
        for (SysFieldPermission perm : permissions) {
            UserPermissionVO.FieldPermissionVO vo = new UserPermissionVO.FieldPermissionVO();
            vo.setFieldId(perm.getFieldId());
            vo.setCanView(perm.getCanView() == 1);
            vo.setCanEdit(perm.getCanEdit() == 1);
            result.put(perm.getFieldId(), vo);
        }
        return result;
    }

    public boolean checkPagePermission(Long userId, Long appId, Long pageId) {
        List<SysPagePermission> permissions = sysPagePermissionMapper.selectPagePermissions(userId, appId, pageId);
        if (permissions.isEmpty()) {
            return true;
        }
        for (SysPagePermission perm : permissions) {
            if (perm.getCanAccess() == 1) {
                return true;
            }
        }
        return false;
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignUserAppRoles(Long userId, Long appId, List<Long> roleIds) {
        sysUserAppRoleMapper.deleteByUserAndApp(userId, appId);

        if (roleIds != null && !roleIds.isEmpty()) {
            Long currentUserId = UserContext.getCurrentUserId();
            for (Long roleId : roleIds) {
                SysUserAppRole uar = new SysUserAppRole();
                uar.setUserId(userId);
                uar.setAppId(appId);
                uar.setRoleId(roleId);
                uar.setCreatedBy(currentUserId);
                sysUserAppRoleMapper.insert(uar);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createRowPermission(RowPermissionDTO dto) {
        RowPermissionExpressionEngine.ExpressionParseResult parseResult =
                expressionEngine.parseExpression(dto.getExpression());
        if (!parseResult.isValid()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, parseResult.getErrorMessage());
        }

        SysRowPermission permission = new SysRowPermission();
        permission.setAppId(dto.getAppId());
        permission.setRoleId(dto.getRoleId());
        permission.setModelId(dto.getModelId());
        permission.setPermissionName(dto.getPermissionName());
        permission.setPermissionCode(dto.getPermissionCode());
        permission.setExpression(dto.getExpression());
        permission.setConditionType(dto.getConditionType() != null ? dto.getConditionType() : "AND");
        permission.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        permission.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        permission.setRemark(dto.getRemark());
        permission.setCreatedBy(UserContext.getCurrentUserId());

        sysRowPermissionMapper.insert(permission);
        return permission.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRowPermission(Long id, RowPermissionDTO dto) {
        if (dto.getExpression() != null) {
            RowPermissionExpressionEngine.ExpressionParseResult parseResult =
                    expressionEngine.parseExpression(dto.getExpression());
            if (!parseResult.isValid()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, parseResult.getErrorMessage());
            }
        }

        SysRowPermission permission = sysRowPermissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }

        if (dto.getRoleId() != null) permission.setRoleId(dto.getRoleId());
        if (dto.getModelId() != null) permission.setModelId(dto.getModelId());
        if (dto.getPermissionName() != null) permission.setPermissionName(dto.getPermissionName());
        if (dto.getPermissionCode() != null) permission.setPermissionCode(dto.getPermissionCode());
        if (dto.getExpression() != null) permission.setExpression(dto.getExpression());
        if (dto.getConditionType() != null) permission.setConditionType(dto.getConditionType());
        if (dto.getPriority() != null) permission.setPriority(dto.getPriority());
        if (dto.getStatus() != null) permission.setStatus(dto.getStatus());
        if (dto.getRemark() != null) permission.setRemark(dto.getRemark());
        permission.setUpdatedBy(UserContext.getCurrentUserId());

        sysRowPermissionMapper.updateById(permission);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRowPermission(Long id) {
        sysRowPermissionMapper.deleteById(id);
    }

    public List<SysRowPermission> getRowPermissions(Long appId, Long roleId, Long modelId) {
        LambdaQueryWrapper<SysRowPermission> wrapper = new LambdaQueryWrapper<>();
        if (appId != null) wrapper.eq(SysRowPermission::getAppId, appId);
        if (roleId != null) wrapper.eq(SysRowPermission::getRoleId, roleId);
        if (modelId != null) wrapper.eq(SysRowPermission::getModelId, modelId);
        wrapper.orderByAsc(SysRowPermission::getPriority);
        return sysRowPermissionMapper.selectList(wrapper);
    }

    public RowPermissionExpressionEngine.ExpressionParseResult validateExpression(String expression) {
        return expressionEngine.parseExpression(expression);
    }

    public boolean evaluateExpression(String expression, Map<String, Object> data) {
        Long userId = UserContext.getCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        return expressionEngine.evaluate(expression, user, data);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveFieldPermissions(Long roleId, Long modelId, List<SysFieldPermission> permissions) {
        sysFieldPermissionMapper.delete(
            new LambdaQueryWrapper<SysFieldPermission>()
                .eq(SysFieldPermission::getRoleId, roleId)
                .eq(SysFieldPermission::getModelId, modelId)
        );

        Long currentUserId = UserContext.getCurrentUserId();
        for (SysFieldPermission perm : permissions) {
            perm.setRoleId(roleId);
            perm.setModelId(modelId);
            perm.setCreatedBy(currentUserId);
            sysFieldPermissionMapper.insert(perm);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePagePermissions(Long roleId, List<SysPagePermission> permissions) {
        sysPagePermissionMapper.delete(
            new LambdaQueryWrapper<SysPagePermission>()
                .eq(SysPagePermission::getRoleId, roleId)
        );

        Long currentUserId = UserContext.getCurrentUserId();
        for (SysPagePermission perm : permissions) {
            perm.setRoleId(roleId);
            perm.setCreatedBy(currentUserId);
            sysPagePermissionMapper.insert(perm);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveComponentPermissions(Long roleId, Long pageId, List<SysComponentPermission> permissions) {
        sysComponentPermissionMapper.delete(
            new LambdaQueryWrapper<SysComponentPermission>()
                .eq(SysComponentPermission::getRoleId, roleId)
                .eq(SysComponentPermission::getPageId, pageId)
        );

        Long currentUserId = UserContext.getCurrentUserId();
        for (SysComponentPermission perm : permissions) {
            perm.setRoleId(roleId);
            perm.setPageId(pageId);
            perm.setCreatedBy(currentUserId);
            sysComponentPermissionMapper.insert(perm);
        }
    }
}
