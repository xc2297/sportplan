package org.example.sportplan.service.agent.tool;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.ExerciseRecordResponse;
import org.example.sportplan.service.ExerciseRecordService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;

// 导出运动记录为 Excel 文件，支持按周/按月导出，上传 COS 返回下载链接
@Component
@RequiredArgsConstructor
public class ExportExerciseRecordsTool extends ExportBaseTool {

    private final ExerciseRecordService exerciseRecordService;

    @Override
    public String getName() {
        return "export_exercise_records";
    }

    @Override
    public String getDescription() {
        return "将运动记录导出为Excel文件。支持导出我的本周(my_week)、小组本周(group_week，仅管理员)、我的本月(my_month)的记录，返回下载链接";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> exportTypeProp = new HashMap<>();
        exportTypeProp.put("type", "string");
        exportTypeProp.put("enum", new String[]{"my_week", "group_week", "my_month"});
        exportTypeProp.put("description", "导出范围：my_week=我的本周,group_week=小组本周(管理员),my_month=我的本月");
        Map<String, Object> props = new HashMap<>();
        props.put("export_type", exportTypeProp);
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", props);
        return params;
    }

    // 所有已登录用户可用
    @Override
    public boolean isAvailable(Long userId) {
        return userId != null;
    }

    // 根据 export_type 计算日期范围，查询运动记录后写入 Excel
    @Override
    protected byte[] generateExcel(Long userId, Map<String, Object> args) throws Exception {
        String exportType = (String) args.getOrDefault("export_type", "my_week");
        LocalDate today = LocalDate.now();
        String startDate;
        String endDate = today.toString();

        // my_month 取本月1号至今，其他（my_week/group_week）取最近7天
        switch (exportType) {
            case "my_month":
                startDate = today.withDayOfMonth(1).toString();
                break;
            default:
                startDate = today.minusDays(6).toString();
                break;
        }

        List<ExerciseRecordResponse> records = exerciseRecordService.getRecordsByRange(userId, startDate, endDate);
        List<RecordRow> rows = new ArrayList<>();
        for (ExerciseRecordResponse r : records) {
            RecordRow row = new RecordRow();
            row.setDate(r.getRecordDate() != null ? r.getRecordDate().toString() : "");
            row.setExerciseType(r.getExerciseTypeName() != null ? r.getExerciseTypeName() : "");
            row.setAmount(r.getAmount());
            row.setScore(r.getScore());
            rows.add(row);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        EasyExcel.write(bos, RecordRow.class).sheet("运动记录").doWrite(rows);
        return bos.toByteArray();
    }

    @Override
    protected String getFileName() {
        return "运动记录.xlsx";
    }

    // 运动记录 Excel 行定义
    @Data
    public static class RecordRow {
        @ExcelProperty("日期")
        private String date;
        @ExcelProperty("运动类型")
        private String exerciseType;
        @ExcelProperty("运动量")
        private java.math.BigDecimal amount;
        @ExcelProperty("积分")
        private java.math.BigDecimal score;
    }
}
