package org.example.sportplan.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointAccountResponse {

    private Long userId;
    private BigDecimal totalEarned;
    private BigDecimal availablePoints;
}
