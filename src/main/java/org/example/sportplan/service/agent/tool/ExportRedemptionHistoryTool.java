package org.example.sportplan.service.agent.tool;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.RedemptionResponse;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.RedemptionService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.*;

// 导出兑换记录为 Excel 文件，支持"我的"和"小组全部"两种范围，上传 COS 返回下载链接
@Component
@RequiredArgsConstructor
public class ExportRedemptionHistoryTool extends ExportBaseTool {

    private final RedemptionService redemptionService;
    private final UserMapper userMapper;

    @Override
    public String getName() {
        return "export_redemption_history";
    }

    @Override
    public String getDescription() {
        return "将兑换记录导出为Excel文件。支持导出我的兑换(mine)或小组全部兑换(group，仅管理员)，返回下载链接";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> scopeProp = new HashMap<>();
        scopeProp.put("type", "string");
        scopeProp.put("enum", new String[]{"mine", "group"});
        scopeProp.put("description", "导出范围：mine=我的兑换记录,group=小组全部兑换记录(管理员)");
        Map<String, Object> props = new HashMap<>();
        props.put("export_scope", scopeProp);
        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", props);
        return params;
    }

    @Override
    public boolean isAvailable(Long userId) {
        return userId != null;
    }

    // 根据 export_scope 查询兑换记录，将英文状态翻译为中文后写入 Excel
    @Override
    protected byte[] generateExcel(Long userId, Map<String, Object> args) throws Exception {
        String scope = (String) args.getOrDefault("export_scope", "mine");
        List<RedemptionResponse> records;
        // group 模式查询小组全部兑换记录（通过 groupId），mine 模式查询当前用户
        if ("group".equals(scope)) {
            User user = userMapper.selectById(userId);
            Long groupId = user != null ? user.getGroupId() : null;
            records = redemptionService.getAllHistory(groupId);
        } else {
            records = redemptionService.getHistory(userId);
        }

        List<RedemptionRow> rows = new ArrayList<>();
        for (RedemptionResponse r : records) {
            RedemptionRow row = new RedemptionRow();
            row.setName(r.getUserName() != null ? r.getUserName() : "");
            row.setRewardName(r.getRewardName());
            row.setPointsCost(r.getPointsCost());
            row.setRewardAmount(r.getRewardAmount());
            // 将英文状态值翻译为中文，Excel 中展示更友好
            String status = r.getStatus();
            if ("active".equals(status)) status = "未使用";
            else if ("used".equals(status)) status = "已使用";
            else if ("cancelled".equals(status)) status = "已取消";
            row.setStatus(status);
            row.setRedeemedAt(r.getRedeemedAt());
            rows.add(row);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        EasyExcel.write(bos, RedemptionRow.class).sheet("兑换记录").doWrite(rows);
        return bos.toByteArray();
    }

    @Override
    protected String getFileName() {
        return "兑换记录.xlsx";
    }

    // 兑换记录 Excel 行定义
    @Data
    public static class RedemptionRow {
        @ExcelProperty("姓名")
        private String name;
        @ExcelProperty("奖励名称")
        private String rewardName;
        @ExcelProperty("消耗积分")
        private Integer pointsCost;
        @ExcelProperty("奖励额度(元)")
        private java.math.BigDecimal rewardAmount;
        @ExcelProperty("状态")
        private String status;
        @ExcelProperty("兑换时间")
        private String redeemedAt;
    }
}
