package org.example.sportplan.service.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.DashboardResponse;
import org.example.sportplan.dto.response.ExerciseRecordResponse;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.DashboardService;
import org.example.sportplan.service.ExerciseRecordService;
import org.example.sportplan.service.GroupService;
import org.example.sportplan.service.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

// 管理员查询指定组员的积分和运动数据
@Component
@RequiredArgsConstructor
public class QueryMemberDataTool implements AgentTool {

    private final UserMapper userMapper;
    private final DashboardService dashboardService;
    private final ExerciseRecordService exerciseRecordService;
    private final GroupService groupService;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "query_member_data";
    }

    @Override
    public String getDescription() {
        return "查询指定组员的积分和运动数据（仅管理员可用）";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> memberNameProp = new HashMap<>();
        memberNameProp.put("type", "string");
        memberNameProp.put("description", "要查询的组员姓名");
        Map<String, Object> memberDataTypeProp = new HashMap<>();
        memberDataTypeProp.put("type", "string");
        memberDataTypeProp.put("enum", new String[]{"score", "today_records"});
        memberDataTypeProp.put("description", "score=积分概览,today_records=今日运动记录");
        Map<String, Object> props = new HashMap<>();
        props.put("member_name", memberNameProp);
        props.put("data_type", memberDataTypeProp);
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", Arrays.asList("member_name", "data_type"));
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
        String memberName = (String) args.get("member_name");
        String dataType = (String) args.getOrDefault("data_type", "score");
        User admin = userMapper.selectById(userId);
        if (admin == null || !groupService.isGroupAdmin(userId, admin.getGroupId())) {
            return "{\"error\":\"无权限查询组员数据\"}";
        }
        // 按姓名查找组员：先精确匹配，再模糊匹配（AI 传入的名字可能不完全准确）
        List<User> members = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getGroupId, admin.getGroupId()));
        User target = null;
        for (User m : members) {
            if (m.getName().equals(memberName)) { target = m; break; }
        }
        if (target == null) {
            String trimmed = memberName != null ? memberName.trim() : "";
            for (User m : members) {
                if (m.getName().contains(trimmed) || trimmed.contains(m.getName())) {
                    target = m; break;
                }
            }
        }
        // 未找到匹配组员时，返回可用的成员名单帮助 AI 引导用户
        if (target == null) {
            StringBuilder nameList = new StringBuilder();
            for (User m : members) nameList.append(m.getName()).append(",");
            return "{\"error\":\"未找到该组员\",\"searched_name\":\"" + memberName
                    + "\",\"available_members\":\"" + nameList + "\"}";
        }
        try {
            String today = LocalDate.now().toString();
            if ("today_records".equals(dataType)) {
                List<ExerciseRecordResponse> records = exerciseRecordService.getRecords(target.getId(), today);
                Map<String, Object> result = new HashMap<>();
                result.put("memberName", memberName);
                result.put("date", today);
                result.put("records", records != null ? records : Collections.emptyList());
                return mapper.writeValueAsString(result);
            }
            DashboardResponse dash = dashboardService.getDashboard(target.getId());
            Map<String, Object> result = new HashMap<>();
            result.put("memberName", memberName);
            result.put("todayScore", dash.getTodayScore());
            result.put("weekScore", dash.getWeekScore());
            result.put("totalEarned", dash.getTotalEarned());
            result.put("availablePoints", dash.getAvailablePoints());
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"查询组员数据失败：" + e.getMessage() + "\"}";
        }
    }
}
