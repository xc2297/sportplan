package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应 DTO
 * 返回给前端的用户基本信息，不包含密码等敏感字段。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    /** 用户ID */
    private Long id;

    /** 用户真实姓名 */
    private String name;

    /** 性别，值为 "male" 或 "female" */
    private String gender;

    /** 是否为管理员（小组管理员） */
    private Boolean isAdmin;

    /** 所属小组ID，null=未加入小组 */
    private Long groupId;

    /** 所属小组名称，null=未加入小组 */
    private String groupName;
}
