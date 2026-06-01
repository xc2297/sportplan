package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private Long creatorId;
    private String creatorName;
    private Integer memberCount;
    private Boolean isMember;
    private Boolean isAdmin;
    private String createdAt;
}
