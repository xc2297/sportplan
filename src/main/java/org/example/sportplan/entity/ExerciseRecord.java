package org.example.sportplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 运动记录实体类
 * 对应数据库表 sp_exercise_record，存储用户每日的运动数据。
 * 核心业务规则：
 * - 每人每天每种运动类型只能有一条记录（通过唯一约束保证）
 * - 重复提交同一用户/日期/运动类型的记录会执行更新（upsert逻辑）
 * - 单项每日积分上限由 ExerciseType.dailyCap 控制，默认10分
 * - 每日所有运动类型积分总和上限为40分
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sp_exercise_record")
public class ExerciseRecord {

    /** 记录唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的用户ID，指向 sp_user 表 */
    private Long userId;

    /** 运动记录日期，精确到天 */
    private LocalDate recordDate;

    /** 关联的运动类型ID，指向 sp_exercise_type 表 */
    private Long exerciseTypeId;

    /** 运动量（如跑步3.5公里、俯卧撑50个），精度8位2小数 */
    private BigDecimal amount;

    /** 本次运动获得的积分数，由运动量 × 性别系数计算得出（受每日上限约束），精度5位2小数 */
    private BigDecimal score;

    /** 运动凭证图片URL（非必传，用户上传的运动截图或照片） */
    private String imageUrl;

    /** 记录创建时间，仅在首次插入时设置 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 记录最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
