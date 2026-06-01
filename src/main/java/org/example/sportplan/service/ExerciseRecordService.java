package org.example.sportplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.ExerciseRecordRequest;
import org.example.sportplan.dto.response.ExerciseRecordResponse;
import org.example.sportplan.entity.ExerciseRecord;
import org.example.sportplan.entity.ExerciseType;
import org.example.sportplan.entity.PointAccount;
import org.example.sportplan.entity.User;
import org.example.sportplan.exception.BusinessException;
import org.example.sportplan.mapper.ExerciseRecordMapper;
import org.example.sportplan.mapper.ExerciseTypeMapper;
import org.example.sportplan.mapper.PointAccountMapper;
import org.example.sportplan.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.sportplan.dto.response.CalendarDayResponse;
import org.example.sportplan.dto.response.DayDetailResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 运动记录服务层
 * 核心业务服务，处理运动记录的提交、查询和删除。
 * 积分计算规则：
 * 1. 积分 = 运动量 × 性别对应系数（男/女不同）
 * 2. 单项每日积分上限由运动类型配置的 dailyCap 决定（默认10分）
 * 3. 每日所有运动类型积分总和上限为40分
 * 4. 同一用户/日期/运动类型的记录唯一，重复提交视为更新（upsert）
 */
@Service
@RequiredArgsConstructor
public class ExerciseRecordService {

    /** 每日总积分上限：40分 */
    private static final BigDecimal DAILY_TOTAL_CAP = new BigDecimal("40");

    private final ExerciseRecordMapper recordMapper;
    private final ExerciseTypeMapper exerciseTypeMapper;
    private final PointAccountMapper accountMapper;
    private final UserMapper userMapper;
    private final GroupService groupService;

    /**
     * 提交运动记录（核心业务方法）
     * 实现积分计算和 upsert 逻辑，步骤如下：
     * 1. 校验用户和运动类型是否存在
     * 2. 根据性别选择积分系数，计算原始积分
     * 3. 应用单项每日积分上限
     * 4. 检查当日总积分是否超过40分上限，超过则截断
     * 5. 执行 upsert（存在则更新，不存在则新增）
     * 6. 差额更新积分账户（新增积分或调整已变更的积分）
     *
     * @param request 运动记录请求，包含用户ID、日期、运动类型ID、运动量
     * @return 运动记录响应
     * @throws BusinessException 当用户不存在、运动类型不存在或已停用时抛出
     */
    @Transactional
    public ExerciseRecordResponse submitRecord(ExerciseRecordRequest request) {
        // 校验用户是否存在
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 校验运动类型是否存在
        ExerciseType type = exerciseTypeMapper.selectById(request.getExerciseTypeId());
        if (type == null) {
            throw new BusinessException("运动类型不存在");
        }
        // 校验运动类型是否启用
        if (!Boolean.TRUE.equals(type.getActive())) {
            throw new BusinessException("该运动类型已停用");
        }
        // 校验运动类型属于用户所在小组或为全局类型
        Long userGroupId = user.getGroupId();
        if (userGroupId != null && type.getGroupId() != null && !userGroupId.equals(type.getGroupId())) {
            throw new BusinessException("该运动类型不属于您所在的小组");
        }
        if (userGroupId == null && type.getGroupId() != null) {
            throw new BusinessException("未加入小组无法使用该运动类型");
        }

        LocalDate recordDate = LocalDate.parse(request.getRecordDate());

        // 根据用户性别选择对应的积分系数
        BigDecimal coefficient = user.getGender() == User.Gender.MALE
                ? type.getMaleCoefficient() : type.getFemaleCoefficient();
        // 计算原始积分 = 运动量 × 系数
        BigDecimal rawScore = request.getAmount().multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
        // 应用单项每日积分上限（不超过 dailyCap）
        BigDecimal score = rawScore.min(new BigDecimal(type.getDailyCap()));

        // 查询该用户当天其他运动类型的积分总和，用于校验每日总上限
        BigDecimal othersTotal = recordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, user.getId())
                        .eq(ExerciseRecord::getRecordDate, recordDate)).stream()
                .filter(r -> !r.getExerciseTypeId().equals(type.getId()))
                .map(ExerciseRecord::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 如果加上本次积分后超过每日总上限40分，则截断到剩余可用额度
        if (othersTotal.add(score).compareTo(DAILY_TOTAL_CAP) > 0) {
            score = DAILY_TOTAL_CAP.subtract(othersTotal);
            // 确保积分不为负数
            if (score.compareTo(BigDecimal.ZERO) < 0) score = BigDecimal.ZERO;
        }

        // upsert 逻辑：查询是否已存在同一用户/日期/运动类型的记录
        ExerciseRecord record = recordMapper.selectOne(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, user.getId())
                        .eq(ExerciseRecord::getRecordDate, recordDate)
                        .eq(ExerciseRecord::getExerciseTypeId, type.getId()));
        if (record == null) {
            record = new ExerciseRecord();
        }

        // 记录旧积分值，用于计算差额更新
        BigDecimal oldScore = record.getScore() != null ? record.getScore() : BigDecimal.ZERO;

        record.setUserId(user.getId());
        record.setRecordDate(recordDate);
        record.setExerciseTypeId(type.getId());
        record.setAmount(request.getAmount());
        record.setScore(score);
        // 设置运动凭证图片（可为空）
        record.setImageUrl(request.getImageUrl());
        if (record.getId() == null) {
            recordMapper.insert(record);
        } else {
            recordMapper.updateById(record);
        }

        // 差额更新积分账户：新增积分为正，调整积分为正或负
        BigDecimal delta = score.subtract(oldScore);
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            updatePointAccount(user.getId(), delta);
        }

        return toResponse(record, type);
    }

    /**
     * 查询指定用户在指定日期的运动记录
     *
     * @param userId 用户ID
     * @param date   日期字符串（格式：yyyy-MM-dd）
     * @return 运动记录响应列表
     */
    public List<ExerciseRecordResponse> getRecords(Long userId, String date) {
        LocalDate recordDate = LocalDate.parse(date);
        return recordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .eq(ExerciseRecord::getRecordDate, recordDate)).stream()
                .map(r -> toResponse(r, exerciseTypeMapper.selectById(r.getExerciseTypeId())))
                .collect(Collectors.toList());
    }

    /**
     * 查询指定用户在指定日期范围内的运动记录
     * 用于仪表盘展示本周运动数据。
     *
     * @param userId    用户ID
     * @param startDate 开始日期字符串（格式：yyyy-MM-dd）
     * @param endDate   结束日期字符串（格式：yyyy-MM-dd）
     * @return 运动记录响应列表
     */
    public List<ExerciseRecordResponse> getRecordsByRange(Long userId, String startDate, String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return recordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .between(ExerciseRecord::getRecordDate, start, end)).stream()
                .map(r -> toResponse(r, exerciseTypeMapper.selectById(r.getExerciseTypeId())))
                .collect(Collectors.toList());
    }

    /**
     * 更新用户积分账户（差额更新）
     * 如果用户没有积分账户则自动创建。
     * 同时更新总获得积分（totalEarned）和可用积分（availablePoints）。
     *
     * @param userId 用户ID
     * @param delta  积分变动值（正数为增加，负数为扣减）
     * @throws BusinessException 当扣减后可用积分为负数时抛出
     */
    private void updatePointAccount(Long userId, BigDecimal delta) {
        PointAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<PointAccount>()
                        .eq(PointAccount::getUserId, userId));
        if (account == null) {
            // 新用户首次提交运动记录时自动创建积分账户
            account = new PointAccount();
            account.setUserId(userId);
        }
        account.setTotalEarned(account.getTotalEarned().add(delta));
        account.setAvailablePoints(account.getAvailablePoints().add(delta));
        // 校验可用积分不能为负数
        if (account.getAvailablePoints().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("可用积分不足");
        }
        if (account.getId() == null) {
            accountMapper.insert(account);
        } else {
            accountMapper.updateById(account);
        }
    }

    /**
     * 删除运动记录
     * 权限规则：普通用户只能删除当天记录，管理员可以删除任意日期的记录。
     * 删除后自动回退对应的积分（从积分账户中扣减）。
     *
     * @param recordId      要删除的记录ID
     * @param currentUserId 当前操作用户ID
     * @throws BusinessException 当记录不存在、用户不存在或权限不足时抛出
     */
    @Transactional
    public void deleteRecord(Long recordId, Long currentUserId) {
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }
        ExerciseRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException("记录不存在");
        }

        boolean isGroupAdmin = groupService.isGroupAdmin(currentUserId, currentUser.getGroupId());

        // 判断是否为惩罚记录
        ExerciseType exerciseType = exerciseTypeMapper.selectById(record.getExerciseTypeId());
        boolean isPenalty = exerciseType != null && "未运动惩罚".equals(exerciseType.getName());

        if (isPenalty) {
            if (!isGroupAdmin) {
                throw new BusinessException("惩罚记录仅小组管理员可删除");
            }
        } else {
            if (!isGroupAdmin) {
                if (!record.getRecordDate().equals(LocalDate.now())) {
                    throw new BusinessException("只能删除当天的记录");
                }
                if (!record.getUserId().equals(currentUserId)) {
                    throw new BusinessException("只能删除自己的记录");
                }
            }
            // 组管理员只能删除同组用户的记录
            if (isGroupAdmin && currentUser.getGroupId() != null) {
                User recordUser = userMapper.selectById(record.getUserId());
                if (recordUser != null && !currentUser.getGroupId().equals(recordUser.getGroupId())) {
                    throw new BusinessException("只能删除本小组成员的记录");
                }
            }
        }

        // 回退积分：将记录对应的积分数从账户中扣减
        BigDecimal delta = record.getScore().negate();
        updatePointAccount(record.getUserId(), delta);

        recordMapper.deleteById(record.getId());
    }

    /**
     * 获取日历视图数据
     * 返回指定月份中，每天各用户的积分汇总。
     * 普通用户只返回自己的数据，管理员返回所有用户的数据。
     *
     * @param userId    当前用户ID
     * @param isAdmin   是否管理员
     * @param yearMonth 年月字符串，格式为 yyyy-MM（如 "2026-05"）
     * @return 日历每日汇总列表，仅包含有数据的日期
     */
    public List<CalendarDayResponse> getCalendarData(Long userId, String yearMonth) {
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        User user = userMapper.selectById(userId);
        Long groupId = user != null ? user.getGroupId() : null;
        boolean isGroupAdmin = groupService.isGroupAdmin(userId, groupId);

        if (isGroupAdmin && groupId != null) {
            // 小组管理员：查询该月所有组员的运动记录
            List<Long> memberIds = groupService.getGroupMemberIds(groupId);
            List<ExerciseRecord> records = recordMapper.selectByDateBetweenAndUserIds(start, end, memberIds);
            // 按日期分组
            Map<LocalDate, List<ExerciseRecord>> byDate = records.stream()
                    .collect(Collectors.groupingBy(ExerciseRecord::getRecordDate));

            List<CalendarDayResponse> result = new ArrayList<>();
            for (Map.Entry<LocalDate, List<ExerciseRecord>> entry : byDate.entrySet()) {
                // 每天内按用户分组，汇总各用户当天总分
                Map<Long, List<ExerciseRecord>> byUser = entry.getValue().stream()
                        .collect(Collectors.groupingBy(ExerciseRecord::getUserId));
                List<CalendarDayResponse.UserDailyScore> userScores = new ArrayList<>();
                for (Map.Entry<Long, List<ExerciseRecord>> ue : byUser.entrySet()) {
                    User u = userMapper.selectById(ue.getKey());
                    String userName = u != null ? u.getName() : "";
                    BigDecimal total = ue.getValue().stream()
                            .map(ExerciseRecord::getScore)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    userScores.add(new CalendarDayResponse.UserDailyScore(ue.getKey(), userName, total));
                }
                // 按积分降序排列，方便前端展示
                userScores.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));
                result.add(new CalendarDayResponse(entry.getKey().toString(), userScores));
            }
            // 按日期升序排列
            result.sort(Comparator.comparing(CalendarDayResponse::getDate));
            return result;
        } else {
            // 普通用户或未分组：只查询自己的记录
            List<ExerciseRecord> records = recordMapper.selectList(
                    new LambdaQueryWrapper<ExerciseRecord>()
                            .eq(ExerciseRecord::getUserId, userId)
                            .between(ExerciseRecord::getRecordDate, start, end));
            User u = userMapper.selectById(userId);
            String userName = u != null ? u.getName() : "";
            // 按日期分组求和
            Map<LocalDate, BigDecimal> byDate = records.stream()
                    .collect(Collectors.groupingBy(ExerciseRecord::getRecordDate,
                            Collectors.reducing(BigDecimal.ZERO, ExerciseRecord::getScore, BigDecimal::add)));
            return byDate.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> new CalendarDayResponse(e.getKey().toString(),
                            Collections.singletonList(
                                    new CalendarDayResponse.UserDailyScore(userId, userName, e.getValue()))))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 获取某天的运动详情
     * 普通用户返回自己的当天明细，管理员返回所有用户的当天明细（对比视图）。
     *
     * @param userId  当前用户ID
     * @param isAdmin 是否管理员
     * @param date    日期字符串，格式为 yyyy-MM-dd
     * @return 当天详情响应
     */
    public DayDetailResponse getDayDetail(Long userId, String date) {
        LocalDate targetDate = LocalDate.parse(date);

        User user = userMapper.selectById(userId);
        Long groupId = user != null ? user.getGroupId() : null;
        boolean isGroupAdmin = groupService.isGroupAdmin(userId, groupId);

        if (isGroupAdmin && groupId != null) {
            // 小组管理员：查询所有组员当天的记录
            List<Long> memberIds = groupService.getGroupMemberIds(groupId);
            List<ExerciseRecord> records = recordMapper.selectByDateAndUserIds(targetDate, memberIds);
            Map<Long, List<ExerciseRecord>> byUser = records.stream()
                    .collect(Collectors.groupingBy(ExerciseRecord::getUserId));
            List<DayDetailResponse.UserDayDetail> userDetails = new ArrayList<>();
            for (Map.Entry<Long, List<ExerciseRecord>> ue : byUser.entrySet()) {
                User member = userMapper.selectById(ue.getKey());
                String userName = member != null ? member.getName() : "";
                BigDecimal total = ue.getValue().stream()
                        .map(ExerciseRecord::getScore)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                // 转换为响应 DTO 列表
                List<ExerciseRecordResponse> recordResponses = ue.getValue().stream()
                        .map(r -> toResponse(r, exerciseTypeMapper.selectById(r.getExerciseTypeId())))
                        .collect(Collectors.toList());
                userDetails.add(new DayDetailResponse.UserDayDetail(ue.getKey(), userName, total, recordResponses));
            }
            // 按积分降序排列
            userDetails.sort((a, b) -> b.getTotalScore().compareTo(a.getTotalScore()));
            return new DayDetailResponse(date, userDetails);
        } else {
            // 普通用户或未分组：只查询自己的记录
            List<ExerciseRecord> records = recordMapper.selectList(
                    new LambdaQueryWrapper<ExerciseRecord>()
                            .eq(ExerciseRecord::getUserId, userId)
                            .eq(ExerciseRecord::getRecordDate, targetDate));
            String userName = user != null ? user.getName() : "";
            BigDecimal total = records.stream()
                    .map(ExerciseRecord::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<ExerciseRecordResponse> recordResponses = records.stream()
                    .map(r -> toResponse(r, exerciseTypeMapper.selectById(r.getExerciseTypeId())))
                    .collect(Collectors.toList());
            return new DayDetailResponse(date,
                    Collections.singletonList(
                            new DayDetailResponse.UserDayDetail(userId, userName, total, recordResponses)));
        }
    }

    /**
     * 将运动记录实体转换为响应 DTO
     * 同时附带运动类型的名称和单位信息，方便前端展示。
     *
     * @param r    运动记录实体
     * @param type 运动类型实体（可能为null）
     * @return 运动记录响应 DTO
     */
    private ExerciseRecordResponse toResponse(ExerciseRecord r, ExerciseType type) {
        return new ExerciseRecordResponse(
                r.getId(), r.getUserId(), r.getRecordDate().toString(),
                r.getExerciseTypeId(),
                type != null ? type.getName() : "",
                type != null ? type.getUnit() : "",
                r.getAmount(), r.getScore(),
                r.getImageUrl()
        );
    }
}
