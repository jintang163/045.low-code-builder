package com.lowcode.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.auth.entity.SysRole;
import com.lowcode.auth.mapper.SysRoleMapper;
import com.lowcode.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysRoleService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    public IPage<SysRole> getRolePage(int pageNum, int pageSize, Long appId, String roleName) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (appId != null) {
            wrapper.eq(SysRole::getAppId, appId);
        }
        if (roleName != null && !roleName.isEmpty()) {
            wrapper.like(SysRole::getRoleName, roleName);
        }
        wrapper.orderByAsc(SysRole::getRoleSort);
        return sysRoleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public List<SysRole> getRolesByAppId(Long appId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (appId != null) {
            wrapper.eq(SysRole::getAppId, appId);
        }
        wrapper.orderByAsc(SysRole::getRoleSort);
        return sysRoleMapper.selectList(wrapper);
    }

    public SysRole getRoleById(Long id) {
        return sysRoleMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createRole(SysRole role) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode())
                .eq(SysRole::getAppId, role.getAppId());
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("角色编码已存在");
        }
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        sysRoleMapper.insert(role);
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long id, SysRole role) {
        SysRole existing = sysRoleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("角色不存在");
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode())
                .eq(SysRole::getAppId, role.getAppId())
                .ne(SysRole::getId, id);
        if (sysRoleMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("角色编码已存在");
        }
        role.setId(id);
        sysRoleMapper.updateById(role);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if ("SYSTEM_ADMIN".equals(role.getRoleType()) || "APP_ADMIN".equals(role.getRoleType())) {
            throw new BusinessException("系统内置角色不可删除");
        }
        sysRoleMapper.deleteById(id);
    }
}
