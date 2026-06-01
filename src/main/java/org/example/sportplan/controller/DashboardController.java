package org.example.sportplan.controller;

import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.AdminWeeklyScoresResponse;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.dto.response.DashboardResponse;
import org.example.sportplan.dto.response.LeaderboardEntry;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.DashboardService;
import org.example.sportplan.service.GroupService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 仪表盘控制器
 * 处理仪表盘相关的数据查询请求，包括个人运动数据汇总和积分排行榜。
 * 所有接口路径前缀：/dashboard（需要登录认证）
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserMapper userMapper;
    private final GroupService groupService;

    /**
     * 获取指定用户的仪表盘数据
     * 包含今日积分、本周积分、本周每日积分明细、累计总积分和可用积分。
     *
     * @param userId 用户ID
     * @return 仪表盘响应数据
     */
    @GetMapping("/{userId}")
    public ApiResponse<DashboardResponse> get(@PathVariable Long userId) {
        return ApiResponse.success(dashboardService.getDashboard(userId));
    }

    /**
     * 获取积分排行榜
     * 列出所有用户的总积分排名，按总积分从高到低排列。
     * 用于展示团队/小组的积分竞争情况。
     *
     * @return 排行榜条目列表
     */
    @GetMapping("/leaderboard")
    public ApiResponse<List<LeaderboardEntry>> leaderboard(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        return ApiResponse.success(dashboardService.getLeaderboard(userId));
    }

    @GetMapping("/admin/weekly-scores")
    public ApiResponse<AdminWeeklyScoresResponse> adminWeeklyScores(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        return ApiResponse.success(dashboardService.getAdminWeeklyScores(userId));
    }
}
