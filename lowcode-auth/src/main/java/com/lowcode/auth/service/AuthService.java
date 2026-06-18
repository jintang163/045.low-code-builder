package com.lowcode.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.auth.dto.LoginDTO;
import com.lowcode.auth.entity.SysUser;
import com.lowcode.auth.mapper.SysUserMapper;
import com.lowcode.auth.util.JwtUtil;
import com.lowcode.auth.util.PasswordUtil;
import com.lowcode.auth.vo.LoginVO;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedissonClient redissonClient;

    public LoginVO login(LoginDTO loginDTO, HttpServletRequest request) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, loginDTO.getUsername())
        );

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getStatus() == 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        if (!passwordUtil.validatePassword(loginDTO.getPassword(), user.getPassword(), user.getSalt())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        List<String> roles = sysUserMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = sysUserMapper.selectPermissionCodesByUserId(user.getId());

        String roleStr = String.join(",", roles);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), roleStr);

        RBucket<String> tokenBucket = redissonClient.getBucket("auth:token:" + user.getId());
        tokenBucket.set(token, 7, TimeUnit.DAYS);

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(request));
        sysUserMapper.updateById(user);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setTokenType("Bearer");
        loginVO.setExpiresIn(7 * 24 * 60 * 60L);
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setNickname(user.getNickname());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRoles(roles);
        loginVO.setPermissions(permissions);

        return loginVO;
    }

    public void logout(String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId != null) {
            RBucket<String> tokenBucket = redissonClient.getBucket("auth:token:" + userId);
            tokenBucket.delete();
        }
    }

    public boolean validateToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            return false;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return false;
        }
        RBucket<String> tokenBucket = redissonClient.getBucket("auth:token:" + userId);
        String storedToken = tokenBucket.get();
        return token.equals(storedToken);
    }

    public SysUser getUserInfoByToken(String token) {
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
        return sysUserMapper.selectById(userId);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
