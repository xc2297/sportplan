package org.example.sportplan.dto.request;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建/更新运动类型请求 DTO
 * 用于管理员创建或更新运动类型的请求数据。
 * 包含运动类型的基本属性和性别差异化的积分系数。
 */
@Data
public class CreateExerciseTypeRequest {

    /** 运动类型名称，如"跑步"、"走路"、"俯卧撑"、"仰卧起坐" */
    @NotBlank(message = "名称不能为空")
    private String name;

    /** 运动计量单位，如"公里"、"个" */
    @NotBlank(message = "单位不能为空")
    private String unit;

    /** 男性积分系数，运动量 × 此系数 = 男性用户的运动积分 */
    @NotNull(message = "男性系数不能为空")
    private BigDecimal maleCoefficient;

    /** 女性积分系数，运动量 × 此系数 = 女性用户的运动积分 */
    @NotNull(message = "女性系数不能为空")
    private BigDecimal femaleCoefficient;

    /** 单项每日积分上限，默认10分 */
    private Integer dailyCap = 10;

    /** 排序序号，值越小越靠前，默认为0 */
    private Integer sortOrder;

    /** 是否启用，true-启用 false-停用，默认启用 */
    private Boolean active = true;
}
