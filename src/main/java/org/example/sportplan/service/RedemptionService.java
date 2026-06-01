package org.example.sportplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.RedemptionRequest;
import org.example.sportplan.dto.request.UseRedemptionRequest;
import org.example.sportplan.dto.response.RedemptionResponse;
import org.example.sportplan.entity.PointAccount;
import org.example.sportplan.entity.RewardItem;
import org.example.sportplan.entity.RedemptionRecord;
import org.example.sportplan.entity.User;
import org.example.sportplan.exception.BusinessException;
import org.example.sportplan.mapper.PointAccountMapper;
import org.example.sportplan.mapper.RewardItemMapper;
import org.example.sportplan.mapper.RedemptionRecordMapper;
import org.example.sportplan.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 兑换服务层
 * 处理奖励兑换的业务逻辑，包括积分兑换、使用核销、撤销和兑换历史查询。
 */
@Service
@RequiredArgsConstructor
public class RedemptionService {

    private final RedemptionRecordMapper redemptionMapper;
    private final PointAccountMapper accountMapper;
    private final RewardItemMapper rewardItemMapper;
    private final UserMapper userMapper;
    private final GroupService groupService;

    /**
     * 执行奖励兑换
     * 校验积分余额是否充足，扣减积分并生成兑换记录。
     *
     * @param request 兑换请求，包含用户ID和奖励项ID
     * @return 兑换记录响应
     */
    @Transactional
    public RedemptionResponse redeem(RedemptionRequest request) {
        RewardItem item = rewardItemMapper.selectById(request.getRewardItemId());
        if (item == null) {
            throw new BusinessException("奖励项不存在");
        }

        PointAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<PointAccount>()
                        .eq(PointAccount::getUserId, request.getUserId()));
        if (account == null) {
            throw new BusinessException("积分账户不存在");
        }

        BigDecimal cost = new BigDecimal(item.getPointsCost());
        if (account.getAvailablePoints().compareTo(cost) < 0) {
            throw new BusinessException("积分不足，当前可用: " + account.getAvailablePoints());
        }

        account.setAvailablePoints(account.getAvailablePoints().subtract(cost));
        accountMapper.updateById(account);

        RedemptionRecord record = new RedemptionRecord();
        record.setUserId(request.getUserId());
        record.setRewardTier(String.valueOf(item.getId()));
        record.setPointsCost(item.getPointsCost());
        record.setRewardName(item.getName());
        record.setRewardAmount(item.getMaxAmount());
        redemptionMapper.insert(record);

        return toResponse(record, null);
    }

    /**
     * 使用兑换券（核销）
     * 用户上传凭证图片和描述后，将兑换记录标记为已使用。
     * 已使用的记录不可撤销。
     *
     * @param redemptionId 兑换记录ID
     * @param request 使用请求，包含描述和凭证图片URL列表
     */
    @Transactional
    public void useRedemption(Long redemptionId, UseRedemptionRequest request) {
        RedemptionRecord record = redemptionMapper.selectById(redemptionId);
        if (record == null) {
            throw new BusinessException("兑换记录不存在");
        }

        if (!"active".equals(record.getStatus())) {
            throw new BusinessException("该兑换券当前状态不可使用");
        }

        record.setStatus("used");
        record.setUsedAt(LocalDateTime.now());
        record.setUsedDescription(request.getDescription());
        record.setUsedImages(String.join(",", request.getImageUrls()));
        redemptionMapper.updateById(record);
    }

    /**
     * 查询用户的兑换历史记录
     *
     * @param userId 用户ID
     * @return 兑换记录响应列表
     */
    public List<RedemptionResponse> getHistory(Long userId) {
        return redemptionMapper.selectList(
                new LambdaQueryWrapper<RedemptionRecord>()
                        .eq(RedemptionRecord::getUserId, userId)
                        .orderByDesc(RedemptionRecord::getRedeemedAt)).stream()
                .map(r -> toResponse(r, null))
                .collect(Collectors.toList());
    }

    /**
     * 查询所有用户的兑换历史记录（管理员使用）
     *
     * @return 所有用户的兑换记录响应列表，包含用户名
     */
    public List<RedemptionResponse> getAllHistory(Long groupId) {
        List<Long> memberIds = groupService.getGroupMemberIds(groupId);
        Map<Long, String> userNameMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
        return redemptionMapper.selectList(
                new LambdaQueryWrapper<RedemptionRecord>()
                        .in(RedemptionRecord::getUserId, memberIds)
                        .orderByDesc(RedemptionRecord::getRedeemedAt)).stream()
                .map(r -> toResponse(r, userNameMap.getOrDefault(r.getUserId(), "")))
                .collect(Collectors.toList());
    }

    /**
     * 撤销兑换（仅管理员可操作）
     * 已使用的记录不可撤销。
     *
     * @param redemptionId 要撤销的兑换记录ID
     */
    @Transactional
    public void cancelRedemption(Long redemptionId) {
        RedemptionRecord record = redemptionMapper.selectById(redemptionId);
        if (record == null) {
            throw new BusinessException("兑换记录不存在");
        }

        if ("cancelled".equals(record.getStatus())) {
            throw new BusinessException("该兑换已被撤销");
        }

        if ("used".equals(record.getStatus())) {
            throw new BusinessException("已使用的兑换券不可撤销");
        }

        BigDecimal refund = new BigDecimal(record.getPointsCost());
        PointAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<PointAccount>()
                        .eq(PointAccount::getUserId, record.getUserId()));
        if (account == null) {
            throw new BusinessException("积分账户不存在");
        }
        account.setAvailablePoints(account.getAvailablePoints().add(refund));
        accountMapper.updateById(account);

        record.setStatus("cancelled");
        record.setCancelledAt(LocalDateTime.now());
        redemptionMapper.updateById(record);
    }

    /**
     * 将兑换记录实体转换为响应 DTO
     * usedImages 从逗号分隔字符串拆分为 List。
     *
     * @param r 兑换记录实体
     * @param userName 用户名，可为null
     */
    private RedemptionResponse toResponse(RedemptionRecord r, String userName) {
        List<String> images = r.getUsedImages() != null && !r.getUsedImages().isEmpty()
                ? Arrays.asList(r.getUsedImages().split(","))
                : Collections.emptyList();

        RedemptionResponse resp = new RedemptionResponse();
        resp.setId(r.getId());
        resp.setUserId(r.getUserId());
        resp.setUserName(userName);
        resp.setRewardTier(r.getRewardTier());
        resp.setRewardName(r.getRewardName());
        resp.setPointsCost(r.getPointsCost());
        resp.setRewardAmount(r.getRewardAmount());
        resp.setRedeemedAt(r.getRedeemedAt().toString());
        resp.setStatus(r.getStatus());
        resp.setCancelledAt(r.getCancelledAt() != null ? r.getCancelledAt().toString() : null);
        resp.setUsedAt(r.getUsedAt() != null ? r.getUsedAt().toString() : null);
        resp.setUsedDescription(r.getUsedDescription());
        resp.setUsedImages(images);
        return resp;
    }
}
