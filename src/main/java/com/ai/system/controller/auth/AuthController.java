package com.ai.system.controller.auth;

import com.ai.system.model.dto.token.TokenDO;
import com.ai.system.mapper.UserMapper;
import com.ai.system.model.entity.User;
import com.ai.system.model.pojo.CommonResult;
import com.ai.system.controller.auth.vo.ForgetPasswordVO;
import com.ai.system.controller.auth.vo.LoginVO;
import com.ai.system.controller.auth.vo.LogoutVO;
import com.ai.system.controller.auth.vo.UpdatePasswordVO;
import com.ai.system.controller.auth.vo.UserRegisterVO;
import com.ai.system.service.AuthService;
import com.ai.system.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserService userService;

    @PostMapping("/login")
    public CommonResult<TokenDO> login(@Valid @RequestBody LoginVO req) {
        TokenDO token = authService.login(req.getUsername(), req.getPassword(), req.getRememberMe());
        return CommonResult.success(token);
    }

    @GetMapping("/me")
    public CommonResult<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        Map<String, Object> info = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "businessName",user.getBusinessName(),
                "role", user.getRole() != null ? user.getRole() : "USER");
        return CommonResult.success(info);
    }

    @PostMapping("/logout")
    public CommonResult<Void> logout(@Valid @RequestBody LogoutVO req) {
        authService.logout(req.getRefreshToken());
        return CommonResult.success(null);
    }

    @PostMapping("/forget-password")
    public CommonResult<Boolean> forgetPassword(@RequestBody ForgetPasswordVO forgotPasswordReq) {
        return CommonResult.success(userService.forgetPassword(forgotPasswordReq));
    }


    @PostMapping("/change-password")
    public CommonResult<Boolean> changePassword(@RequestBody UpdatePasswordVO updatePasswordReq) {
        return CommonResult.success(userService.changePassword(updatePasswordReq));
    }

    @PostMapping("/register")
    public CommonResult<Long> register(@Valid @RequestBody UserRegisterVO req) {
        Long userId = userService.register(req);
        return CommonResult.success(userId);
    }
}
