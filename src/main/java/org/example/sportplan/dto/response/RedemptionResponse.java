package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 兑换记录响应 DTO
 * 返回给前端的兑换记录信息，用于展示兑换历史。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedemptionResponse {

    /** 兑换记录ID */
    private Long id;

    /** 兑换用户ID */
    private Long userId;

    /** 兑换用户姓名（管理员查看全部记录时展示） */
    private String userName;

    /** 奖励等级标识 */
    private String rewardTier;

    /** 奖励名称（冗余存储，防止奖励项被修改后历史记录丢失名称） */
    private String rewardName;

    /** 本次兑换消耗的积分数 */
    private Integer pointsCost;

    /** 奖励对应的金额价值 */
    private BigDecimal rewardAmount;

    /** 兑换时间，格式为 ISO 日期时间字符串 */
    private String redeemedAt;

    /** 兑换状态：active=已兑换，cancelled=已撤销，used=已使用 */
    private String status;

    /** 撤销时间，未撤销时为null */
    private String cancelledAt;

    /** 使用时间，未使用时为null */
    private String usedAt;

    /** 使用描述，用户填写的消费说明 */
    private String usedDescription;

    /** 使用凭证图片URL列表 */
    private List<String> usedImages;
}
