package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinRequestResponse {

    private Long id;
    private Long groupId;
    private String groupName;
    private Long userId;
    private String userName;
    private String status;
    private String createdAt;
}
