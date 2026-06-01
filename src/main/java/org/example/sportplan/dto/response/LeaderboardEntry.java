package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 排行榜条目响应 DTO
 * 用于积分排行榜中展示每个用户的积分排名信息。
 * 按总积分从高到低排列。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {

    /** 用户ID */
    private Long userId;

    /** 用户真实姓名 */
    private String name;

    /** 性别，值为 "male" 或 "female" */
    private String gender;

    /** 累计获得的总积分 */
    private BigDecimal totalEarned;

    /** 当前可用积分 */
    private BigDecimal availablePoints;
}
