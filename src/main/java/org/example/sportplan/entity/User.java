package org.example.sportplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 * 对应数据库表 sp_user，存储系统用户的基本信息，包括登录凭证、个人信息和管理员标识。
 * 用于登录认证、运动记录关联和权限控制。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sp_user")
public class User {

    /** 用户唯一标识，自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号，全局唯一，最长50字符 */
    private String username;

    /** 登录密码，存储 SHA-256 哈希值 */
    private String password;

    /** 用户真实姓名，最长50字符 */
    private String name;

    /** 性别枚举（MALE-男 / FEMALE-女），用于运动积分系数的差异化计算 */
    private Gender gender;

    /** 是否为管理员（已废弃，改用小组管理员机制） */
    @TableField("is_admin")
    private Boolean isAdmin;

    /** 所属小组ID，null表示未加入任何小组 */
    private Long groupId;

    /** 是否为所在小组的管理员 */
    @TableField("is_group_admin")
    private Boolean isGroupAdmin;

    /** 记录创建时间，仅在首次插入时设置，后续不可更新 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 记录最后更新时间，每次修改时自动刷新 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 性别枚举
     * MALE - 男性
     * FEMALE - 女性
     * 性别不同会影响运动积分的计算系数（如跑步男性系数0.5，女性系数1.0）
     */
    public enum Gender {
        MALE, FEMALE
    }
}
