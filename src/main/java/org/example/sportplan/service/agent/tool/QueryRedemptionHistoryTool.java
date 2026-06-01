package org.example.sportplan.service.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.service.RedemptionService;
import org.example.sportplan.service.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.*;

// 查询当前用户的兑换记录
@Component
@RequiredArgsConstructor
public class QueryRedemptionHistoryTool implements AgentTool {

    private final RedemptionService redemptionService;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "query_redemption_history";
    }

    @Override
    public String getDescription() {
        return "查询当前用户的兑换券记录，包括已兑换、已使用、已取消的兑换券";
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
        return userId != null;
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        try {
            return mapper.writeValueAsString(redemptionService.getHistory(userId));
        } catch (Exception e) {
            return "{\"error\":\"查询兑换记录失败：" + e.getMessage() + "\"}";
        }
    }
}
