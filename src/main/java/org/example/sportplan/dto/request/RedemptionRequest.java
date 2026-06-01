package org.example.sportplan.dto.request;

import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * 奖励兑换请求 DTO
 * 用于接收前端提交奖励兑换的请求数据。
 * 系统会校验用户积分是否充足，充足则扣减积分并生成兑换记录。
 */
@Data
public class RedemptionRequest {

    /** 用户ID，标识哪个用户要兑换奖励 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 奖励项ID，标识要兑换哪个奖励 */
    @NotNull(message = "奖励项ID不能为空")
    private Long rewardItemId;
}
