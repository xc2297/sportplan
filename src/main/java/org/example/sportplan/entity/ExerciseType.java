package org.example.sportplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运动类型实体类
 * 对应数据库表 sp_exercise_type，存储系统中支持的各种运动项目配置。
 * 每种运动类型包含名称、计量单位、性别差异化的积分系数和每日积分上限。
 * 例如：跑步（公里，男0.5/女1.0）、俯卧撑（个，男0.1/女0.1）等。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sp_exercise_type")
public class ExerciseType {

    /** 运动类型唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 运动类型名称，如"跑步"、"走路"、"俯卧撑"、"仰卧起坐"，最长50字符 */
    private String name;

    /** 运动计量单位，如"公里"、"个"，最长10字符 */
    private String unit;

    /** 男性积分系数，运动量 × 系数 = 积分，精度6位2小数 */
    private BigDecimal maleCoefficient;

    /** 女性积分系数，运动量 × 系数 = 积分，精度6位2小数 */
    private BigDecimal femaleCoefficient;

    /** 单项运动每日积分上限，默认10分 */
    private Integer dailyCap = 10;

    /** 排序序号，用于前端展示时的排列顺序，值越小越靠前 */
    private Integer sortOrder;

    /** 是否启用，true-启用中 false-已停用，停用后用户不可提交该类型的运动记录 */
    private Boolean active = true;

    /** 所属小组ID，null为全局模板 */
    private Long groupId;

    /** 记录创建时间，仅在首次插入时设置 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 记录最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
