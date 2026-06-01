package org.example.sportplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.CreateRewardItemRequest;
import org.example.sportplan.dto.response.RewardItemResponse;
import org.example.sportplan.entity.RewardItem;
import org.example.sportplan.exception.BusinessException;
import org.example.sportplan.mapper.RewardItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 奖励项服务层
 * 处理奖励项的业务逻辑，包括查询、创建、更新和删除奖励项。
 * 仅管理员有权限创建、更新和删除奖励项。
 * 奖励项定义了可兑换的奖品及其所需积分。
 */
@Service
@RequiredArgsConstructor
public class RewardItemService {

    private final RewardItemMapper rewardItemMapper;

    /**
     * 获取所有奖励项列表
     * 按排序序号升序排列，供用户端兑换中心和管理端使用。
     *
     * @return 奖励项响应列表
     */
    public List<RewardItemResponse> getAll(Long groupId) {
        if (groupId != null) {
            return rewardItemMapper.selectList(new LambdaQueryWrapper<RewardItem>()
                    .eq(RewardItem::getGroupId, groupId)
                    .orderByAsc(RewardItem::getSortOrder)).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return rewardItemMapper.selectList(new LambdaQueryWrapper<RewardItem>()
                .isNull(RewardItem::getGroupId)
                .orderByAsc(RewardItem::getSortOrder)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建新的奖励项
     *
     * @param request 创建请求，包含名称、描述、所需积分、额度等
     * @return 创建成功的奖励项响应
     */
    @Transactional
    public RewardItemResponse create(Long groupId, CreateRewardItemRequest request) {
        RewardItem item = new RewardItem();
        item.setName(request.getName());
        item.setGroupId(groupId);
        item.setDescription(request.getDescription());
        item.setPointsCost(request.getPointsCost());
        item.setMaxAmount(request.getMaxAmount());
        // 未指定排序时默认为0
        item.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        rewardItemMapper.insert(item);
        return toResponse(item);
    }

    /**
     * 更新奖励项信息
     * 仅在请求中提供了排序值时才更新排序字段。
     *
     * @param id      奖励项ID
     * @param request 更新请求
     * @return 更新后的奖励项响应
     * @throws BusinessException 当奖励项不存在时抛出
     */
    @Transactional
    public RewardItemResponse update(Long id, CreateRewardItemRequest request) {
        RewardItem item = rewardItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("奖励项不存在");
        }
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPointsCost(request.getPointsCost());
        item.setMaxAmount(request.getMaxAmount());
        // 仅在明确提供了排序值时才更新
        if (request.getSortOrder() != null) {
            item.setSortOrder(request.getSortOrder());
        }
        rewardItemMapper.updateById(item);
        return toResponse(item);
    }

    /**
     * 删除奖励项
     * 物理删除，直接从数据库中移除记录。
     *
     * @param id 奖励项ID
     * @throws BusinessException 当奖励项不存在时抛出
     */
    @Transactional
    public void delete(Long id) {
        if (rewardItemMapper.selectById(id) == null) {
            throw new BusinessException("奖励项不存在");
        }
        rewardItemMapper.deleteById(id);
    }

    /**
     * 根据ID获取奖励项实体
     * 供兑换服务（RedemptionService）在兑换时获取奖励项信息。
     *
     * @param id 奖励项ID
     * @return 奖励项实体
     * @throws BusinessException 当奖励项不存在时抛出
     */
    public RewardItem getById(Long id) {
        RewardItem item = rewardItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("奖励项不存在");
        }
        return item;
    }

    /**
     * 将奖励项实体转换为响应 DTO
     *
     * @param item 奖励项实体
     * @return 奖励项响应 DTO
     */
    private RewardItemResponse toResponse(RewardItem item) {
        return new RewardItemResponse(item.getId(), item.getName(), item.getDescription(),
                item.getPointsCost(), item.getMaxAmount(), item.getSortOrder(), item.getGroupId());
    }
}
