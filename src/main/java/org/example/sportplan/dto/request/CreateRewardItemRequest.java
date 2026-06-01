package org.example.sportplan.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建/更新奖励项请求 DTO
 * 用于管理员创建或更新可兑换奖励项的请求数据。
 */
@Data
public class CreateRewardItemRequest {

    /** 奖励名称，如"即时畅饮券"、"豪华大餐券" */
    @NotBlank(message = "奖励名称不能为空")
    private String name;

    /** 奖励描述，说明奖励的用途和使用场景 */
    private String description;

    /** 兑换所需积分数（如5分、10分、20分、30分） */
    @NotNull(message = "所需积分不能为空")
    private Integer pointsCost;

    /** 奖励对应的金额（元），标识奖励的价值上限 */
    @NotNull(message = "额度不能为空")
    private BigDecimal maxAmount;

    /** 排序序号，值越小越靠前 */
    private Integer sortOrder;
}
