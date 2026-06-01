package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 运动记录响应 DTO
 * 返回给前端的运动记录信息，附带运动类型名称和单位，方便前端展示。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseRecordResponse {

    /** 记录ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 记录日期，格式为 yyyy-MM-dd */
    private String recordDate;

    /** 运动类型ID */
    private Long exerciseTypeId;

    /** 运动类型名称（冗余字段，方便前端展示） */
    private String exerciseTypeName;

    /** 运动计量单位（冗余字段，如"公里"、"个"） */
    private String unit;

    /** 运动量（如3.5公里、50个） */
    private BigDecimal amount;

    /** 本次运动获得的积分数（受每日上限约束后的最终值） */
    private BigDecimal score;

    /** 运动凭证图片URL（可为空） */
    private String imageUrl;
}
