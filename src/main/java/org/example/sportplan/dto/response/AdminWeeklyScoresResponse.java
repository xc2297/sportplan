package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminWeeklyScoresResponse {

    private List<UserWeeklyLine> users;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWeeklyLine {
        private Long userId;
        private String name;
        private List<DashboardResponse.DailyScore> scores;
    }
}
