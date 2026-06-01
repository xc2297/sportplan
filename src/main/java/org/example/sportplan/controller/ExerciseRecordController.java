package org.example.sportplan.controller;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.ExerciseRecordRequest;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.dto.response.CalendarDayResponse;
import org.example.sportplan.dto.response.DayDetailResponse;
import org.example.sportplan.dto.response.ExerciseRecordResponse;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.UserMapper;
import org.example.sportplan.service.ExerciseRecordService;
import org.example.sportplan.service.GroupService;
import org.example.sportplan.service.PenaltyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 运动记录控制器
 * 处理运动记录的提交、查询和删除请求。
 * 所有接口路径前缀：/exercise-records（需要登录认证）
 */
@RestController
@RequestMapping("/exercise-records")
@RequiredArgsConstructor
public class ExerciseRecordController {

    private final ExerciseRecordService recordService;
    private final UserMapper userMapper;
    private final PenaltyService penaltyService;
    private final GroupService groupService;

    /**
     * 提交运动记录
     * 支持新增和更新（upsert），同一用户/日期/运动类型的记录重复提交会更新。
     * 积分自动计算，受单项每日上限和每日总上限约束。
     *
     * @param request 运动记录请求，包含用户ID、日期、运动类型ID和运动量
     * @return 运动记录响应，包含计算后的积分
     */
    @PostMapping
    public ApiResponse<ExerciseRecordResponse> submit(@Valid @RequestBody ExerciseRecordRequest request) {
        return ApiResponse.success(recordService.submitRecord(request));
    }

    /**
     * 查询指定用户在指定日期的运动记录
     *
     * @param userId 用户ID
     * @param date   日期（格式：yyyy-MM-dd）
     * @return 运动记录响应列表
     */
    @GetMapping
    public ApiResponse<List<ExerciseRecordResponse>> get(@RequestParam Long userId,
                                                          @RequestParam String date) {
        return ApiResponse.success(recordService.getRecords(userId, date));
    }

    /**
     * 查询指定用户在指定日期范围内的运动记录
     * 用于仪表盘展示本周运动数据等场景。
     *
     * @param userId    用户ID
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate   结束日期（格式：yyyy-MM-dd）
     * @return 运动记录响应列表
     */
    @GetMapping("/range")
    public ApiResponse<List<ExerciseRecordResponse>> getRange(
            @RequestParam Long userId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ApiResponse.success(recordService.getRecordsByRange(userId, startDate, endDate));
    }

    /**
     * 删除运动记录
     * 普通用户只能删除当天记录，管理员可删除任意记录。
     * 删除后自动回退对应积分。
     *
     * @param id         要删除的记录ID
     * @param httpRequest HTTP请求对象，用于获取当前登录用户ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest httpRequest) {
        // 从Session中获取当前登录用户ID
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        recordService.deleteRecord(id, userId);
        return ApiResponse.success();
    }

    @GetMapping("/calendar")
    public ApiResponse<List<CalendarDayResponse>> getCalendar(
            @RequestParam String yearMonth,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        return ApiResponse.success(recordService.getCalendarData(userId, yearMonth));
    }

    /**
     * 获取某天的运动详情
     * 小组管理员看组内所有用户，普通用户看自己。
     */
    @GetMapping("/day-detail")
    public ApiResponse<DayDetailResponse> getDayDetail(
            @RequestParam String date,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        return ApiResponse.success(recordService.getDayDetail(userId, date));
    }

    /**
     * 手动触发惩罚检查（仅管理员）
     * 用于补录过去某天的惩罚记录。
     *
     * @param date      要检查的日期，格式 yyyy-MM-dd
     * @param httpRequest HTTP请求
     * @return 被扣分的用户数量
     */
    @PostMapping("/penalty")
    public ApiResponse<Integer> triggerPenalty(
            @RequestParam String date,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getSession(false).getAttribute("userId");
        User user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (!groupService.isGroupAdmin(userId, user.getGroupId())) {
            return ApiResponse.error(403, "仅小组管理员可执行此操作");
        }
        int count = penaltyService.manualPenaltyCheck(date);
        return ApiResponse.success(count);
    }
}
