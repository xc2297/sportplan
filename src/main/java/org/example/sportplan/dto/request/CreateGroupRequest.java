package org.example.sportplan.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateGroupRequest {

    @NotBlank(message = "小组名称不能为空")
    private String name;

    private String description;
}
