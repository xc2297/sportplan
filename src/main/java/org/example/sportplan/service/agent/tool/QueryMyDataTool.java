package org.example.sportplan.service.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.DashboardResponse;
import org.example.sportplan.dto.response.ExerciseRecordResponse;
import org.example.sportplan.service.DashboardService;
import org.example.sportplan.service.ExerciseRecordService;
import org.example.sportplan.service.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

// 查询当前用户自己的积分和运动数据
@Component
@RequiredArgsConstructor
public class QueryMyDataTool implements AgentTool {

    private final DashboardService dashboardService;
    private final ExerciseRecordService exerciseRecordService;
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() {
        return "query_my_data";
    }

    @Override
    public String getDescription() {
        return "查询当前用户自己的积分和运动数据";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> dataTypeProp = new HashMap<>();
        dataTypeProp.put("type", "string");
        dataTypeProp.put("enum", new String[]{"score", "today_records", "week_records"});
        dataTypeProp.put("description", "score=积分概览,today_records=今日运动记录,week_records=近7天运动记录");
        Map<String, Object> props = new HashMap<>();
        props.put("data_type", dataTypeProp);
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", props);
        params.put("required", Collections.singletonList("data_type"));
        return params;
    }

    @Override
    public boolean isAvailable(Long userId) {
        return userId != null;
    }

    // 根据 data_type 分别查询积分概览、今日记录或本周记录
    @Override
    public String execute(Map<String, Object> args, Long userId) {
        String dataType = (String) args.getOrDefault("data_type", "score");
        String today = LocalDate.now().toString();
        try {
            // 查询今日运动记录列表
            if ("today_records".equals(dataType)) {
                List<ExerciseRecordResponse> records = exerciseRecordService.getRecords(userId, today);
                Map<String, Object> result = new HashMap<>();
                result.put("date", today);
                result.put("records", records != null ? records : Collections.emptyList());
                return mapper.writeValueAsString(result);
            }
            // 查询近7天运动记录
            if ("week_records".equals(dataType)) {
                String weekStart = LocalDate.now().minusDays(6).toString();
                List<ExerciseRecordResponse> records = exerciseRecordService.getRecordsByRange(userId, weekStart, today);
                Map<String, Object> result = new HashMap<>();
                result.put("start", weekStart);
                result.put("end", today);
                result.put("records", records != null ? records : Collections.emptyList());
                return mapper.writeValueAsString(result);
            }
            // 默认：查询积分概览（今日积分、本周积分、累计积分、可用积分）
            DashboardResponse dash = dashboardService.getDashboard(userId);
            Map<String, Object> result = new HashMap<>();
            result.put("todayScore", dash.getTodayScore());
            result.put("weekScore", dash.getWeekScore());
            result.put("totalEarned", dash.getTotalEarned());
            result.put("availablePoints", dash.getAvailablePoints());
            if (dash.getWeekDailyScores() != null) {
                result.put("dailyScores", dash.getWeekDailyScores());
            }
            return mapper.writeValueAsString(result);
        } catch (Exception e) {
            return "{\"error\":\"查询失败：" + e.getMessage() + "\"}";
        }
    }
}
