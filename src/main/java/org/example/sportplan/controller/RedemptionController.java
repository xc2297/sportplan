package org.example.sportplan.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.RedemptionRequest;
import org.example.sportplan.dto.request.UseRedemptionRequest;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.dto.response.RedemptionResponse;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.GroupService;
import org.example.sportplan.service.RedemptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 兑换记录控制器
 * 处理奖励兑换、使用核销、撤销和兑换历史查询请求。
 */
@RestController
@RequestMapping("/redemptions")
@RequiredArgsConstructor
public class RedemptionController {

    private final RedemptionService redemptionService;
    private final UserMapper userMapper;
    private final GroupService groupService;

    @PostMapping
    public ApiResponse<RedemptionResponse> redeem(@Valid @RequestBody RedemptionRequest request) {
        return ApiResponse.success(redemptionService.redeem(request));
    }

    @GetMapping
    public ApiResponse<List<RedemptionResponse>> history(
            @RequestParam(required = false) Long userId,
            HttpServletRequest httpRequest) {
        if (userId == null) {
            Long sessionUserId = (Long) httpRequest.getSession(false).getAttribute("userId");
            User user = userMapper.selectById(sessionUserId);
            if (user == null) throw new RuntimeException("用户不存在");
            if (groupService.isGroupAdmin(sessionUserId, user.getGroupId())) {
                return ApiResponse.success(redemptionService.getAllHistory(user.getGroupId()));
            }
            return ApiResponse.success(redemptionService.getHistory(sessionUserId));
        }
        return ApiResponse.success(redemptionService.getHistory(userId));
    }

    /**
     * 使用兑换券（核销）
     * 用户提交使用描述和凭证图片后，将兑换记录标记为已使用。
     *
     * @param id      兑换记录ID
     * @param request 使用请求，包含描述和图片URL列表
     * @return 空响应
     */
    @PutMapping("/{id}/use")
    public ApiResponse<Void> use(@PathVariable Long id, @Valid @RequestBody UseRedemptionRequest request) {
        redemptionService.useRedemption(id, request);
        return ApiResponse.success();
    }

    /**
     * 撤销兑换（仅管理员）
     * 已使用的兑换券不可撤销。
     *
     * @param id         兑换记录ID
     * @param httpRequest HTTP请求，用于获取当前用户并校验管理员权限
     * @return 空响应
     */
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (!groupService.isGroupAdmin(userId, user.getGroupId())) {
            return ApiResponse.error(403, "仅小组管理员可撤销兑换");
        }
        redemptionService.cancelRedemption(id);
        return ApiResponse.success();
    }
}
