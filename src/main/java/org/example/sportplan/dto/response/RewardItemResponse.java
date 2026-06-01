package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 奖励项响应 DTO
 * 返回给前端的奖励项信息，用于兑换中心展示可兑换的奖励。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardItemResponse {

    /** 奖励项ID */
    private Long id;

    /** 奖励名称，如"即时畅饮券"、"豪华大餐券" */
    private String name;

    /** 奖励描述 */
    private String description;

    /** 兑换所需积分数 */
    private Integer pointsCost;

    /** 奖励对应的金额价值（元） */
    private BigDecimal maxAmount;

    /** 排序序号 */
    private Integer sortOrder;

    /** 所属小组ID，null=全局模板 */
    private Long groupId;
}
