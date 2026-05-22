package com.ai.system.service;

import com.ai.system.controller.dashboard.vo.DashboardStatsDO;

public interface DashboardService {
    public DashboardStatsDO getAdminStats(String startTime, String endTime);

    public DashboardStatsDO getUserStats(Long userId, String startTime, String endTime);
}
