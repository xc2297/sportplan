package org.example.sportplan.controller;

import lombok.RequiredArgsConstructor;
import org.example.sportplan.config.AuthInterceptor;
import org.example.sportplan.dto.request.CreateRewardItemRequest;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.dto.response.RewardItemResponse;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.RewardItemService;
import org.example.sportplan.service.GroupService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 奖励项控制器
 * 处理奖励项的查询、创建、更新和删除请求。
 * 查询接口对所有登录用户开放（供兑换中心展示），增删改接口仅管理员可操作。
 * 所有接口路径前缀：/reward-items（需要登录认证）
 */
@RestController
@RequestMapping("/reward-items")
@RequiredArgsConstructor
public class RewardItemController {

    private final RewardItemService rewardItemService;
    private final UserMapper userMapper;
    private final GroupService groupService;

    /**
     * 查询所有奖励项列表
     * 供兑换中心展示所有可兑换的奖励，按排序序号升序排列。
     *
     * @return 奖励项响应列表
     */
    @GetMapping
    public ApiResponse<List<RewardItemResponse>> list(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        User user = userMapper.selectById(userId);
        Long groupId = user != null ? user.getGroupId() : null;
        return ApiResponse.success(rewardItemService.getAll(groupId));
    }

    /**
     * 创建新的奖励项（仅管理员）
     *
     * @param request    创建请求，包含名称、描述、所需积分、额度等
     * @param httpRequest HTTP请求对象，用于权限校验
     * @return 创建成功的奖励项响应
     */
    @PostMapping
    public ApiResponse<RewardItemResponse> create(@Valid @RequestBody CreateRewardItemRequest request,
                                                   HttpServletRequest httpRequest) {
        Long groupId = checkGroupAdmin(httpRequest);
        return ApiResponse.success(rewardItemService.create(groupId, request));
    }

    /**
     * 更新奖励项（仅管理员）
     *
     * @param id         奖励项ID
     * @param request    更新请求
     * @param httpRequest HTTP请求对象，用于权限校验
     * @return 更新后的奖励项响应
     */
    @PutMapping("/{id}")
    public ApiResponse<RewardItemResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CreateRewardItemRequest request,
                                                   HttpServletRequest httpRequest) {
        checkGroupAdmin(httpRequest);
        return ApiResponse.success(rewardItemService.update(id, request));
    }

    /**
     * 删除奖励项（仅管理员）
     *
     * @param id         奖励项ID
     * @param httpRequest HTTP请求对象，用于权限校验
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        checkGroupAdmin(httpRequest);
        rewardItemService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 管理员权限校验
     * 从 Session 中获取当前用户，检查是否具有管理员权限。
     *
     * @param request HTTP请求对象
     * @throws RuntimeException 当用户不存在或无管理员权限时抛出
     */
    private Long checkGroupAdmin(HttpServletRequest request) {
        Long userId = (Long) request.getSession(false).getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (user.getGroupId() == null) {
            throw new RuntimeException("您未加入任何小组");
        }
        if (!groupService.isGroupAdmin(userId, user.getGroupId())) {
            throw new RuntimeException("无小组管理员权限");
        }
        return user.getGroupId();
    }
}
