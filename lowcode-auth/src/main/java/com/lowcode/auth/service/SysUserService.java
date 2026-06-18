package com.lowcode.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.auth.entity.SysUser;
import com.lowcode.auth.entity.SysUserRole;
import com.lowcode.auth.mapper.SysUserMapper;
import com.lowcode.auth.mapper.SysUserRoleMapper;
import com.lowcode.auth.util.PasswordUtil;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    public IPage<SysUser> getUserPage(int pageNum, int pageSize, String username, Integer status) {
        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectPage(page, wrapper);
    }

    public SysUser getUserById(Long id) {
        return sysUserMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createUser(SysUser user, List<Long> roleIds) {
        SysUser existUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername())
        );
        if (existUser != null) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        String salt = passwordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(passwordUtil.encryptPassword(user.getPassword(), salt));
        sysUserMapper.insert(user);

        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user, List<Long> roleIds) {
        sysUserMapper.updateById(user);

        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId())
        );

        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        sysUserMapper.deleteById(id);
        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
        );
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        String salt = passwordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(passwordUtil.encryptPassword(newPassword, salt));
        sysUserMapper.updateById(user);
    }

    public List<String> getUserRoles(Long userId) {
        return sysUserMapper.selectRoleCodesByUserId(userId);
    }

    public List<String> getUserPermissions(Long userId) {
        return sysUserMapper.selectPermissionCodesByUserId(userId);
    }
}
