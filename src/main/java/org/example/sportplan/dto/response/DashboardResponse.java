package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 仪表盘响应 DTO
 * 返回给前端仪表盘页面的汇总数据，包含今日/本周/总积分和本周每日积分明细。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    /** 今日运动获得的总积分 */
    private BigDecimal todayScore;

    /** 本周运动获得的总积分（从本周一到今天） */
    private BigDecimal weekScore;

    /** 累计获得的总积分（历史所有运动积分之和） */
    private BigDecimal totalEarned;

    /** 当前可用积分（可用于兑换奖励，兑换后扣减） */
    private BigDecimal availablePoints;

    /** 本周每日积分明细列表（从本周一到今天，每天一条） */
    private List<DailyScore> weekDailyScores;

    /**
     * 每日积分明细内部类
     * 表示某一天的运动积分合计。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyScore {

        /** 日期，格式为 yyyy-MM-dd */
        private String date;

        /** 当天所有运动的总积分 */
        private BigDecimal score;
    }
}
