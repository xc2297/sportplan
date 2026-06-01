package org.example.sportplan.dto.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运动记录提交请求 DTO
 * 用于接收前端提交运动记录的请求数据。
 * 同一用户/日期/运动类型的记录唯一，重复提交视为更新（upsert）。
 */
@Data
public class ExerciseRecordRequest {

    /** 用户ID，标识是哪个用户的运动记录 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 记录日期，格式为 yyyy-MM-dd（如 "2024-01-15"） */
    @NotNull(message = "记录日期不能为空")
    private String recordDate;

    /** 运动类型ID，关联 sp_exercise_type 表 */
    @NotNull(message = "运动类型ID不能为空")
    private Long exerciseTypeId;

    /** 运动量（如跑步3.5公里、俯卧撑50个），由前端根据运动类型的单位输入 */
    @NotNull(message = "运动量不能为空")
    private BigDecimal amount;

    /** 运动凭证图片URL（非必传，上传图片后获得的访问路径） */
    private String imageUrl;
}
