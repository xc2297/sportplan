package org.example.sportplan.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.ApiResponse;
import org.example.sportplan.entity.Notification;
import org.example.sportplan.mapper.NotificationMapper;
import org.example.sportplan.service.ReminderService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// 通知控制器：查询未读通知、标记已读
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationMapper notificationMapper;
    private final ReminderService reminderService;

    // 查询当前用户的未读通知
    @GetMapping
    public ApiResponse<List<Notification>> getUnread(HttpServletRequest request) {
        Long userId = (Long) request.getSession(false).getAttribute("userId");
        List<Notification> notifications = notificationMapper.selectList(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getRead, false)
                        .orderByDesc(Notification::getCreatedAt));
        return ApiResponse.success(notifications);
    }

    // 标记单条通知已读
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getSession(false).getAttribute("userId");
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getRead, true));
        return ApiResponse.success(null);
    }

    // 全部标记已读
    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getSession(false).getAttribute("userId");
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getRead, false)
                .set(Notification::getRead, true));
        return ApiResponse.success(null);
    }

    // 手动触发运动提醒（管理员用，测试通知流程）
    @PostMapping("/trigger-reminder")
    public ApiResponse<String> triggerReminder() {
        reminderService.sendExerciseReminder();
        return ApiResponse.success("提醒已触发");
    }
}
