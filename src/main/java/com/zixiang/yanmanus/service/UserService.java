package com.zixiang.yanmanus.service;

import com.zixiang.yanmanus.dto.AuthResponse;
import com.zixiang.yanmanus.dto.LoginRequest;
import com.zixiang.yanmanus.dto.RegisterRequest;
import com.zixiang.yanmanus.entity.User;
import com.zixiang.yanmanus.exception.BusinessException;
import com.zixiang.yanmanus.repository.UserRepository;
import com.zixiang.yanmanus.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("邮箱已被注册");
        }
        if (request.email() == null || request.email().isBlank()
                || !request.email().contains("@")) {
            throw new BusinessException("邮箱格式不正确");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new BusinessException("密码长度不能少于6位");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("邮箱或密码错误"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("邮箱或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token);
    }
}
