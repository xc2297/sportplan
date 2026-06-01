package org.example.sportplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 兑换记录实体类
 * 对应数据库表 sp_redemption_record，存储用户兑换奖励的历史记录。
 * 每次用户成功兑换奖励后生成一条记录，包含兑换的奖励信息、消耗积分和兑换时间。
 * 兑换流程：扣减用户可用积分 -> 生成兑换记录
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sp_redemption_record")
public class RedemptionRecord {

    /** 兑换记录唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 兑换用户ID，指向 sp_user 表 */
    private Long userId;

    /** 奖励等级标识，存储对应的奖励项ID */
    private String rewardTier;

    /** 本次兑换消耗的积分数 */
    private Integer pointsCost;

    /** 奖励名称（冗余存储，防止奖励项修改后历史记录丢失名称） */
    private String rewardName;

    /** 奖励对应的金额价值，精度8位2小数 */
    private BigDecimal rewardAmount;

    /** 兑换时间，仅在首次插入时设置 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime redeemedAt;

    /** 兑换状态：active=已兑换，cancelled=已撤销，used=已使用 */
    private String status = "active";

    /** 撤销时间，撤销时设置 */
    private LocalDateTime cancelledAt;

    /** 使用时间，核销时设置 */
    private LocalDateTime usedAt;

    /** 使用描述，用户填写的消费说明 */
    private String usedDescription;

    /** 使用凭证图片URL列表，逗号分隔 */
    private String usedImages;
}
