package com.zixiang.yanmanus.controller;

import com.zixiang.yanmanus.dto.AuthResponse;
import com.zixiang.yanmanus.dto.LoginRequest;
import com.zixiang.yanmanus.dto.RegisterRequest;
import com.zixiang.yanmanus.dto.Response;
import com.zixiang.yanmanus.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户认证", description = "注册与登录接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Response<Void> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return Response.ok();
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Response<AuthResponse> login(@RequestBody LoginRequest request) {
        return Response.ok(userService.login(request));
    }
}
