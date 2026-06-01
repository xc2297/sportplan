package org.example.sportplan.service.agent.tool;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.LeaderboardEntry;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.DashboardService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.*;

// 导出小组积分排行榜为 Excel 文件，上传 COS 返回下载链接
@Component
@RequiredArgsConstructor
public class ExportLeaderboardTool extends ExportBaseTool {

    private final DashboardService dashboardService;
    private final UserMapper userMapper;

    @Override
    public String getName() {
        return "export_leaderboard";
    }

    @Override
    public String getDescription() {
        return "将小组积分排行榜导出为Excel文件，返回下载链接";
    }

    // 无额外参数（排行榜数据由用户所属小组自动确定）
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", new HashMap<>());
        return params;
    }

    // 权限：已加入小组的用户才能导出排行榜
    @Override
    public boolean isAvailable(Long userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        return user != null && user.getGroupId() != null;
    }

    // 查询排行榜数据，将性别英文值翻译为中文后写入 Excel
    @Override
    protected byte[] generateExcel(Long userId, Map<String, Object> args) throws Exception {
        List<LeaderboardEntry> leaderboard = dashboardService.getLeaderboard(userId);
        List<LeaderboardRow> rows = new ArrayList<>();
        int rank = 1;
        for (LeaderboardEntry entry : leaderboard) {
            LeaderboardRow row = new LeaderboardRow();
            row.setRank(rank++);
            row.setName(entry.getName());
            row.setGender("male".equals(entry.getGender()) ? "男" : "女");
            row.setTotalEarned(entry.getTotalEarned());
            row.setAvailablePoints(entry.getAvailablePoints());
            rows.add(row);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        EasyExcel.write(bos, LeaderboardRow.class).sheet("排行榜").doWrite(rows);
        return bos.toByteArray();
    }

    @Override
    protected String getFileName() {
        return "排行榜.xlsx";
    }

    // 排行榜 Excel 行定义，@ExcelProperty 指定列头名称
    @Data
    public static class LeaderboardRow {
        @ExcelProperty("排名")
        private int rank;
        @ExcelProperty("姓名")
        private String name;
        @ExcelProperty("性别")
        private String gender;
        @ExcelProperty("累计积分")
        private java.math.BigDecimal totalEarned;
        @ExcelProperty("可用积分")
        private java.math.BigDecimal availablePoints;
    }
}
