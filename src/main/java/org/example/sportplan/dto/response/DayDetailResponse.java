package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 日历某天详情响应 DTO
 * 点击日历上的某一天后，返回当天各用户的运动明细。
 * 普通用户只返回自己的明细，管理员返回所有用户的明细（对比视图）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayDetailResponse {

    /** 日期，格式为 yyyy-MM-dd */
    private String date;

    /** 当天各用户的运动明细列表 */
    private List<UserDayDetail> users;

    /**
     * 用户某天运动明细内部类
     * 包含用户基本信息和该天所有运动记录。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDayDetail {

        /** 用户ID */
        private Long userId;

        /** 用户姓名 */
        private String userName;

        /** 当天运动总积分 */
        private BigDecimal totalScore;

        /** 当天所有运动记录明细列表 */
        private List<ExerciseRecordResponse> records;
    }
}
