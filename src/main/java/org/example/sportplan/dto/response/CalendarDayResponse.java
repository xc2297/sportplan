package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 日历视图每日汇总响应 DTO
 * 用于个人中心日历视图，返回某一天各用户的积分汇总。
 * 普通用户只包含自己的数据，管理员包含所有用户的数据。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayResponse {

    /** 日期，格式为 yyyy-MM-dd */
    private String date;

    /** 当天各用户的积分汇总列表 */
    private List<UserDailyScore> users;

    /**
     * 用户每日积分汇总内部类
     * 表示某一天某位用户的总积分。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDailyScore {

        /** 用户ID */
        private Long userId;

        /** 用户姓名 */
        private String userName;

        /** 当天运动总积分 */
        private BigDecimal totalScore;
    }
}
