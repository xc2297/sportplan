package org.example.sportplan.controller;

import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.CreateExerciseTypeRequest;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.dto.response.ExerciseTypeResponse;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.ExerciseTypeService;
import org.example.sportplan.service.GroupService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 运动类型控制器
 * 处理运动类型的查询、创建、更新和删除请求。
 * 查询接口对所有登录用户开放，增删改接口仅管理员可操作。
 * 所有接口路径前缀：/exercise-types（需要登录认证）
 */
@RestController
@RequestMapping("/exercise-types")
@RequiredArgsConstructor
public class ExerciseTypeController {

    private final ExerciseTypeService exerciseTypeService;
    private final UserMapper userMapper;
    private final GroupService groupService;

    /**
     * 查询运动类型列表
     * 支持通过参数控制是否只返回启用中的运动类型。
     *
     * @param activeOnly 是否只返回启用中的类型，true-仅启用中，null/false-全部
     * @return 运动类型响应列表
     */
    @GetMapping
    public ApiResponse<List<ExerciseTypeResponse>> list(
            @RequestParam(required = false) Boolean activeOnly,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        User user = userMapper.selectById(userId);
        Long groupId = user != null ? user.getGroupId() : null;
        if (Boolean.TRUE.equals(activeOnly)) {
            return ApiResponse.success(exerciseTypeService.getActive(groupId));
        }
        return ApiResponse.success(exerciseTypeService.getAll(groupId));
    }

    /**
     * 创建新的运动类型（仅管理员）
     *
     * @param request    创建请求，包含名称、单位、性别系数等
     * @param httpRequest HTTP请求对象，用于权限校验
     * @return 创建成功的运动类型响应
     */
    @PostMapping
    public ApiResponse<ExerciseTypeResponse> create(@Valid @RequestBody CreateExerciseTypeRequest request,
                                                     HttpServletRequest httpRequest) {
        Long groupId = checkGroupAdmin(httpRequest);
        return ApiResponse.success(exerciseTypeService.create(groupId, request));
    }

    /**
     * 更新运动类型（仅管理员）
     *
     * @param id         运动类型ID
     * @param request    更新请求
     * @param httpRequest HTTP请求对象，用于权限校验
     * @return 更新后的运动类型响应
     */
    @PutMapping("/{id}")
    public ApiResponse<ExerciseTypeResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody CreateExerciseTypeRequest request,
                                                     HttpServletRequest httpRequest) {
        Long groupId = checkGroupAdmin(httpRequest);
        return ApiResponse.success(exerciseTypeService.update(id, request));
    }

    /**
     * 删除运动类型（仅管理员）
     *
     * @param id         运动类型ID
     * @param httpRequest HTTP请求对象，用于权限校验
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        checkGroupAdmin(httpRequest);
        exerciseTypeService.delete(id);
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
