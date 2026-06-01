package org.example.sportplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.response.AdminWeeklyScoresResponse;
import org.example.sportplan.dto.response.DashboardResponse;
import org.example.sportplan.dto.response.LeaderboardEntry;
import org.example.sportplan.entity.ExerciseRecord;
import org.example.sportplan.entity.PointAccount;
import org.example.sportplan.entity.User;
import org.example.sportplan.exception.BusinessException;
import org.example.sportplan.mapper.ExerciseRecordMapper;
import org.example.sportplan.mapper.PointAccountMapper;
import org.example.sportplan.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 仪表盘服务层
 * 处理仪表盘相关的业务逻辑，包括个人运动数据汇总和排行榜。
 * 提供今日积分、本周积分、每日积分明细、总积分和可用积分等数据。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExerciseRecordMapper recordMapper;
    private final PointAccountMapper accountMapper;
    private final UserMapper userMapper;
    private final GroupService groupService;

    /**
     * 获取用户仪表盘数据
     * 汇总今日积分、本周积分、每日积分明细和积分账户信息。
     * 本周定义为从本周一到今天。
     *
     * @param userId 用户ID
     * @return 仪表盘响应，包含今日积分、本周积分、总积分、可用积分和本周每日积分明细
     */
    public DashboardResponse getDashboard(Long userId) {
        LocalDate today = LocalDate.now();
        // 近7天起始日期（含今天共7天）
        LocalDate weekStart = today.minusDays(6);

        // 计算今日积分：聚合用户当天所有运动记录的积分
        BigDecimal todayScore = recordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .eq(ExerciseRecord::getRecordDate, today)).stream()
                .map(ExerciseRecord::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算本周每日积分明细：从本周一到今天，逐天聚合
        List<ExerciseRecord> weekRecords = recordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .between(ExerciseRecord::getRecordDate, weekStart, today));
        List<DashboardResponse.DailyScore> dailyScores = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(today); d = d.plusDays(1)) {
            final LocalDate date = d;
            // 过滤出当天的记录并求和
            BigDecimal score = weekRecords.stream()
                    .filter(r -> r.getRecordDate().equals(date))
                    .map(ExerciseRecord::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dailyScores.add(new DashboardResponse.DailyScore(date.toString(), score));
        }

        // 计算本周总积分
        BigDecimal weekScore = dailyScores.stream()
                .map(DashboardResponse.DailyScore::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 获取积分账户信息
        PointAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<PointAccount>()
                        .eq(PointAccount::getUserId, userId));
        if (account == null) {
            // 没有积分账户时返回默认值（积分账户在首次提交运动记录时创建）
            account = new PointAccount();
            account.setTotalEarned(BigDecimal.ZERO);
            account.setAvailablePoints(BigDecimal.ZERO);
        }

        return new DashboardResponse(todayScore, weekScore, account.getTotalEarned(),
                account.getAvailablePoints(), dailyScores);
    }

    /**
     * 获取积分排行榜
     * 列出所有用户的总积分和可用积分，按总积分从高到低排列。
     * 用于展示团队/小组的积分排名。
     *
     * @return 排行榜条目列表，按总积分降序排列
     */
    public List<LeaderboardEntry> getLeaderboard(Long currentUserId) {
        User currentUser = userMapper.selectById(currentUserId);
        Long groupId = currentUser != null ? currentUser.getGroupId() : null;

        List<User> users;
        if (groupId != null) {
            users = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getGroupId, groupId));
        } else {
            users = Collections.singletonList(currentUser);
        }

        return users.stream().map(user -> {
            PointAccount account = accountMapper.selectOne(
                    new LambdaQueryWrapper<PointAccount>()
                            .eq(PointAccount::getUserId, user.getId()));
            if (account == null) {
                // 没有积分账户时返回默认值
                account = new PointAccount();
                account.setTotalEarned(BigDecimal.ZERO);
                account.setAvailablePoints(BigDecimal.ZERO);
            }
            return new LeaderboardEntry(user.getId(), user.getName(),
                    user.getGender().name().toLowerCase(), account.getTotalEarned(),
                    account.getAvailablePoints());
        }).sorted((a, b) -> b.getTotalEarned().compareTo(a.getTotalEarned()))
                .collect(Collectors.toList());
    }

    public AdminWeeklyScoresResponse getAdminWeeklyScores(Long currentUserId) {
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }
        Long groupId = currentUser.getGroupId();
        if (!groupService.isGroupAdmin(currentUserId, groupId)) {
            throw new BusinessException("无权限访问");
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        List<Long> memberIds = groupService.getGroupMemberIds(groupId);
        List<User> groupUsers = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getGroupId, groupId));
        List<ExerciseRecord> allRecords = recordMapper.selectByDateBetweenAndUserIds(weekStart, today, memberIds);

        Map<Long, List<ExerciseRecord>> recordsByUser = allRecords.stream()
                .collect(Collectors.groupingBy(ExerciseRecord::getUserId));

        List<AdminWeeklyScoresResponse.UserWeeklyLine> lines = new ArrayList<>();
        for (User user : groupUsers) {
            List<ExerciseRecord> userRecords = recordsByUser.getOrDefault(user.getId(), new ArrayList<>());
            List<DashboardResponse.DailyScore> dailyScores = new ArrayList<>();
            for (LocalDate d = weekStart; !d.isAfter(today); d = d.plusDays(1)) {
                final LocalDate date = d;
                BigDecimal score = userRecords.stream()
                        .filter(r -> r.getRecordDate().equals(date))
                        .map(ExerciseRecord::getScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                dailyScores.add(new DashboardResponse.DailyScore(date.toString(), score));
            }
            lines.add(new AdminWeeklyScoresResponse.UserWeeklyLine(user.getId(), user.getName(), dailyScores));
        }

        return new AdminWeeklyScoresResponse(lines);
    }
}
