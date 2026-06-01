package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {

    private Long userId;
    private String name;
    private String gender;
    private Boolean isAdmin;
}
