package org.example.sportplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 奖励项实体类
 * 对应数据库表 sp_reward_item，存储可兑换的奖励商品/券信息。
 * 系统预设4个奖励等级：
 * - 5分 - 即时畅饮券（奶茶/咖啡等饮品）
 * - 10分 - 欢乐聚餐券
 * - 20分 - 休闲娱乐券（电影/下午茶等）
 * - 30分 - 豪华大餐券
 * 用户可使用运动积分兑换奖励项。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sp_reward_item")
public class RewardItem {

    /** 奖励项唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 奖励名称，如"即时畅饮券"、"豪华大餐券"，最长100字符 */
    private String name;

    /** 奖励描述，说明奖励的用途和使用场景，最长200字符 */
    private String description;

    /** 兑换所需积分数，如5分、10分、20分、30分 */
    private Integer pointsCost;

    /** 奖励对应的金额（元），用于标识奖励的价值上限，精度8位2小数 */
    private BigDecimal maxAmount;

    /** 排序序号，用于前端展示时的排列顺序，值越小越靠前 */
    private Integer sortOrder;

    /** 所属小组ID，null为全局模板 */
    private Long groupId;

    /** 记录创建时间，仅在首次插入时设置 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 记录最后更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
