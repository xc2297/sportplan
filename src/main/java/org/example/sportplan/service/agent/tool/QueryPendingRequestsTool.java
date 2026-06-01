package org.example.sportplan.service.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.GroupService;
import org.example.sportplan.service.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.*;

// 查询待审批的入组申请（管理员专用）
@Component
@RequiredArgsConstructor
public class QueryPendingRequestsTool implements AgentTool {

    private final GroupService groupService;
    private final UserMapper userMapper;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "query_pending_requests";
    }

    @Override
    public String getDescription() {
        return "查询待审批的入组申请列表，返回申请人姓名和申请时间（仅管理员可用）";
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
        return user != null && user.getGroupId() != null
                && groupService.isGroupAdmin(userId, user.getGroupId());
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        try {
            User user = userMapper.selectById(userId);
            Long groupId = user.getGroupId();
            return mapper.writeValueAsString(groupService.getPendingRequests(groupId, userId));
        } catch (Exception e) {
            return "{\"error\":\"查询入组申请失败：" + e.getMessage() + "\"}";
        }
    }
}
