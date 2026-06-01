package org.example.sportplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分账户实体类
 * 对应数据库表 sp_point_account，存储每个用户的积分账户信息。
 * 每个用户有且仅有一个积分账户（通过 user_id 唯一约束保证）。
 * 积分变动规则：
 * - 提交运动记录时增加积分（totalEarned 和 availablePoints 同时增加）
 * - 删除运动记录时回退积分（差额扣减）
 * - 兑换奖励时扣减可用积分（仅 availablePoints 减少，totalEarned 不变）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sp_point_account")
public class PointAccount {

    /** 账户唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户ID，全局唯一，一个用户只有一个积分账户 */
    private Long userId;

    /** 累计获得的总积分，只会增加不会减少（兑换奖励不影响此值） */
    private BigDecimal totalEarned = BigDecimal.ZERO;

    /** 当前可用积分，可用于兑换奖励，兑换后扣减 */
    private BigDecimal availablePoints = BigDecimal.ZERO;

    /** 记录创建时间，仅在首次插入时设置 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 记录最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
