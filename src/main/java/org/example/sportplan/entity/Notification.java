package org.example.sportplan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 通知实体类：存储系统推送的通知消息（如未运动提醒）
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sp_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 接收通知的用户ID
    private Long userId;

    // 通知标题
    private String title;

    // 通知内容
    private String content;

    // 是否已读：0=未读，1=已读（read 是 MySQL 保留字，需用反引号转义）
    @TableField("`read`")
    private Boolean read;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
