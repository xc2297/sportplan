package org.example.sportplan.service.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.DashboardService;
import org.example.sportplan.service.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.*;

// 查询小组积分排行榜
@Component
@RequiredArgsConstructor
public class QueryLeaderboardTool implements AgentTool {

    private final DashboardService dashboardService;
    private final UserMapper userMapper;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "query_leaderboard";
    }

    @Override
    public String getDescription() {
        return "查询小组积分排行榜，返回所有组员的累计积分和可用积分排名";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", new HashMap<>());
        return params;
    }

    @Override
    public boolean isAvailable(Long userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        return user != null && user.getGroupId() != null;
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("leaderboard", dashboardService.getLeaderboard(userId));
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"查询排行榜失败：" + e.getMessage() + "\"}";
        }
    }
}
