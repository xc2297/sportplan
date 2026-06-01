package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 运动类型响应 DTO
 * 返回给前端的运动类型配置信息，包含性别差异化的积分系数。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseTypeResponse {

    /** 运动类型ID */
    private Long id;

    /** 运动类型名称，如"跑步"、"俯卧撑" */
    private String name;

    /** 运动计量单位，如"公里"、"个" */
    private String unit;

    /** 男性积分系数 */
    private BigDecimal maleCoefficient;

    /** 女性积分系数 */
    private BigDecimal femaleCoefficient;

    /** 单项每日积分上限 */
    private Integer dailyCap;

    /** 排序序号 */
    private Integer sortOrder;

    /** 是否启用 */
    private Boolean active;

    /** 所属小组ID，null=全局模板 */
    private Long groupId;
}
