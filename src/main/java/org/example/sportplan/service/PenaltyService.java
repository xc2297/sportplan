package org.example.sportplan.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.sportplan.entity.ExerciseRecord;
import org.example.sportplan.entity.ExerciseType;
import org.example.sportplan.entity.PointAccount;
import org.example.sportplan.entity.User;
import org.example.sportplan.mapper.ExerciseRecordMapper;
import org.example.sportplan.mapper.ExerciseTypeMapper;
import org.example.sportplan.mapper.PointAccountMapper;
import org.example.sportplan.mapper.UserMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 惩罚定时任务服务
 * 每天凌晨 0:05 自动检查所有用户前一天是否有运动记录，
 * 未记录运动的用户扣减 1 分可用积分，并生成惩罚运动记录（仅管理员可删除）。
 */
// 未运动惩罚服务：每日自动检查并扣减未运动用户的积分
@Slf4j
@Service
@RequiredArgsConstructor
public class PenaltyService {

    private final UserMapper userMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final ExerciseTypeMapper exerciseTypeMapper;
    private final PointAccountMapper pointAccountMapper;

    /** 每日未运动的惩罚积分数 */
    private static final BigDecimal PENALTY_POINTS = new BigDecimal("1");

    /**
     * 每天凌晨 0:05 执行惩罚检查
     * 检查前一天（昨天）所有用户是否有运动记录，未记录的扣减积分并生成惩罚记录。
     */
    @Scheduled(cron = "0 5 0 * * ?", zone = "Asia/Shanghai")
    @Transactional
    public void dailyPenaltyCheck() {
        executePenalty(LocalDate.now().minusDays(1));
    }

    /**
     * 手动触发指定日期的惩罚检查（管理员调用）
     *
     * @param dateStr 日期字符串，格式 yyyy-MM-dd
     * @return 被扣分的用户数量
     */
    @Transactional
    public int manualPenaltyCheck(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        return executePenalty(date);
    }

    /**
     * 执行指定日期的惩罚逻辑
     *
     * @param targetDate 要检查的日期
     * @return 被扣分的用户数量
     */
    private int executePenalty(LocalDate targetDate) {
        log.info("开始执行惩罚检查，检查日期：{}", targetDate);

        // 获取惩罚运动类型
        ExerciseType penaltyType = exerciseTypeMapper.selectOne(
                new LambdaQueryWrapper<ExerciseType>().eq(ExerciseType::getName, "未运动惩罚"));
        if (penaltyType == null) {
            log.warn("惩罚运动类型不存在，跳过惩罚检查");
            return 0;
        }

        List<User> allUsers = userMapper.selectList(null);
        if (allUsers.isEmpty()) {
            log.info("无注册用户，跳过惩罚检查");
            return 0;
        }

        // 获取目标日期有运动记录的用户ID（排除惩罚类型）
        List<Long> activeUserIds = exerciseRecordMapper.selectActiveUserIdsByDate(targetDate);
        Set<Long> activeUserIdSet = activeUserIds.stream().collect(Collectors.toSet());

        int penalizedCount = 0;
        for (User user : allUsers) {
            if (activeUserIdSet.contains(user.getId())) {
                continue;
            }

            // 获取积分账户
            PointAccount account = pointAccountMapper.selectOne(
                    new LambdaQueryWrapper<PointAccount>().eq(PointAccount::getUserId, user.getId()));
            if (account == null) {
                account = new PointAccount();
                account.setUserId(user.getId());
            }

            // 扣减可用积分
            account.setAvailablePoints(account.getAvailablePoints().subtract(PENALTY_POINTS));
            if (account.getId() == null) {
                pointAccountMapper.insert(account);
            } else {
                pointAccountMapper.updateById(account);
            }

            // 生成惩罚运动记录（score 为负数，用于展示）
            ExerciseRecord penaltyRecord = new ExerciseRecord();
            penaltyRecord.setUserId(user.getId());
            penaltyRecord.setRecordDate(targetDate);
            penaltyRecord.setExerciseTypeId(penaltyType.getId());
            penaltyRecord.setAmount(BigDecimal.ONE);
            penaltyRecord.setScore(PENALTY_POINTS.negate()); // -1
            exerciseRecordMapper.insert(penaltyRecord);

            penalizedCount++;
            log.info("用户[{}]({}) {}未运动，扣减 {} 分", user.getName(), user.getId(), targetDate, PENALTY_POINTS);
        }

        log.info("惩罚检查完成，日期：{}，共 {} 人被扣分", targetDate, penalizedCount);
        return penalizedCount;
    }
}
