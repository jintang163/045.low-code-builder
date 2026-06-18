package com.lowcode.auth.controller;

import com.lowcode.auth.dto.LoginDTO;
import com.lowcode.auth.service.AuthService;
import com.lowcode.auth.vo.LoginVO;
import com.lowcode.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Api(tags = "认证接口")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @ApiOperation("登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        return Result.success(authService.login(loginDTO, request));
    }

    @ApiOperation("登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return Result.success();
    }

    @ApiOperation("验证Token")
    @GetMapping("/validate")
    public Result<Boolean> validateToken(@RequestParam String token) {
        return Result.success(authService.validateToken(token));
    }

    @ApiOperation("获取用户信息")
    @GetMapping("/userInfo")
    public Result<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return Result.success(authService.getUserInfoByToken(token));
    }
}
