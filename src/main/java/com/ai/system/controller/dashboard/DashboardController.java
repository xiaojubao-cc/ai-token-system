package com.ai.system.controller.dashboard;

import com.ai.system.mapper.UserMapper;
import com.ai.system.model.entity.User;
import com.ai.system.model.pojo.CommonResult;
import com.ai.system.controller.dashboard.vo.DashboardStatsDO;
import com.ai.system.service.DashboardService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Resource
    private UserMapper userMapper;

    @GetMapping("/admin/stats")
    public CommonResult<DashboardStatsDO> adminStats(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        return CommonResult.success(dashboardService.getAdminStats(startTime, endTime));
    }

    @GetMapping("/user/stats")
    public CommonResult<DashboardStatsDO> userStats(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, auth.getName()));
        Long userId = user != null ? user.getId() : 0L;
        return CommonResult.success(dashboardService.getUserStats(userId, startTime, endTime));
    }
}
