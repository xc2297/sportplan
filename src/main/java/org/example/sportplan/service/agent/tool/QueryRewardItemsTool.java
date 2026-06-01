package org.example.sportplan.service.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.RewardItemService;
import org.example.sportplan.service.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.*;

// 查询可用的奖励兑换项
@Component
@RequiredArgsConstructor
public class QueryRewardItemsTool implements AgentTool {

    private final RewardItemService rewardItemService;
    private final UserMapper userMapper;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "query_reward_items";
    }

    @Override
    public String getDescription() {
        return "查询当前用户小组可用的奖励兑换项列表，包括名称、描述、所需积分和额度";
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
            Long groupId = null;
            if (userId != null) {
                User user = userMapper.selectById(userId);
                if (user != null) groupId = user.getGroupId();
            }
            return mapper.writeValueAsString(rewardItemService.getAll(groupId));
        } catch (Exception e) {
            return "{\"error\":\"查询奖励列表失败：" + e.getMessage() + "\"}";
        }
    }
}
