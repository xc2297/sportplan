package org.example.sportplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sportplan.entity.Notification;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.ExerciseRecordMapper;
import org.example.sportplan.mapper.NotificationMapper;
import org.example.sportplan.mapper.UserMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// 运动提醒服务：每天18:00检查未运动用户，生成提醒通知
@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final UserMapper userMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final NotificationMapper notificationMapper;

    // 每天18:00执行，提醒当天还没运动记录的用户
    @Scheduled(cron = "0 0 18 * * ?", zone = "Asia/Shanghai")
    public void sendExerciseReminder() {
        LocalDate today = LocalDate.now();
        log.info("开始执行运动提醒检查，日期：{}", today);

        // 查询今天有运动记录的用户ID（排除惩罚类型记录）
        Set<Long> activeUserIds = exerciseRecordMapper.selectActiveUserIdsByDate(today)
                .stream().collect(Collectors.toSet());

        // 查询所有用户
        List<User> allUsers = userMapper.selectList(null);
        int count = 0;
        for (User user : allUsers) {
            if (!activeUserIds.contains(user.getId())) {
                // 检查今天是否已经发过提醒，避免重复
                Long existCount = notificationMapper.selectCount(
                        new LambdaQueryWrapper<Notification>()
                                .eq(Notification::getUserId, user.getId())
                                .eq(Notification::getTitle, "运动提醒")
                                .ge(Notification::getCreatedAt, today.atStartOfDay()));
                if (existCount == 0) {
                    Notification notification = new Notification();
                    notification.setUserId(user.getId());
                    notification.setTitle("运动提醒");
                    notification.setContent("今天还没有运动记录哦，快去记录吧！");
                    notification.setRead(false);
                    notificationMapper.insert(notification);
                    count++;
                }
            }
        }
        log.info("运动提醒完成，共发送{}条提醒", count);
    }
}
