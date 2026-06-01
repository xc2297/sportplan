package org.example.sportplan.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 使用兑换券请求 DTO
 * 用户核销兑换券时提交的数据，包括使用描述和凭证图片。
 * 使用后兑换券状态变为 used，不可再撤销。
 */
@Data
public class UseRedemptionRequest {

    /** 使用描述，记录消费场景说明（必填） */
    @NotBlank(message = "使用描述不能为空")
    private String description;

    /** 凭证图片URL列表，至少上传1张（必填） */
    @NotEmpty(message = "请至少上传1张凭证图片")
    private List<String> imageUrls;
}
