package org.example.sportplan.service.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.GroupService;
import org.example.sportplan.service.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.*;

// 查询小组信息和成员列表
@Component
@RequiredArgsConstructor
public class QueryGroupInfoTool implements AgentTool {

    private final UserMapper userMapper;
    private final GroupService groupService;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "query_group_info";
    }

    @Override
    public String getDescription() {
        return "查询当前用户所在小组的信息和成员列表，返回小组名称、成员姓名和角色";
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
            User user = userMapper.selectById(userId);
            Long groupId = user.getGroupId();
            Map<String, Object> result = new HashMap<>();
            result.put("groupId", groupId);
            result.put("groupName", groupService.getGroup(groupId, userId).getName());
            result.put("members", groupService.getMembers(groupId, userId));
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"查询小组信息失败：" + e.getMessage() + "\"}";
        }
    }
}
