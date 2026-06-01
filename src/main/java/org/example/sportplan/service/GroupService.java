package org.example.sportplan.service;

import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.CreateGroupRequest;
import org.example.sportplan.dto.response.GroupMemberResponse;
import org.example.sportplan.dto.response.GroupResponse;
import org.example.sportplan.dto.response.JoinRequestResponse;
import org.example.sportplan.entity.ExerciseRecord;
import org.example.sportplan.entity.ExerciseType;
import org.example.sportplan.entity.GroupJoinRequest;
import org.example.sportplan.entity.RewardItem;
import org.example.sportplan.entity.User;
import org.example.sportplan.entity.UserGroup;
import org.example.sportplan.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.sportplan.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// 小组管理服务：处理创建、加入审批、退出、解散、管理员设置等小组相关业务
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupMapper groupMapper;
    private final UserMapper userMapper;
    private final ExerciseTypeMapper exerciseTypeMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final RewardItemMapper rewardItemMapper;
    private final GroupJoinRequestMapper joinRequestMapper;

    // 创建小组：创建者自动成为管理员，全局运动类型和奖励模板复制到小组
    @Transactional
    public GroupResponse createGroup(Long creatorUserId, CreateGroupRequest request) {
        User creator = userMapper.selectById(creatorUserId);
        if (creator == null) {
            throw new BusinessException("用户不存在");
        }

        if (creator.getGroupId() != null) {
            throw new BusinessException("您已在小组中，无法创建新小组");
        }

        UserGroup existing = groupMapper.selectOne(new LambdaQueryWrapper<UserGroup>().eq(UserGroup::getName, request.getName()));
        if (existing != null) {
            throw new BusinessException("小组名称已存在");
        }

        UserGroup group = new UserGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setCreatorId(creatorUserId);
        groupMapper.insert(group);

        creator.setGroupId(group.getId());
        creator.setIsGroupAdmin(true);
        userMapper.updateById(creator);

        // 复制全局运动类型到小组（排除惩罚类型）
        List<ExerciseType> globalTypes = exerciseTypeMapper.selectList(new LambdaQueryWrapper<ExerciseType>().isNull(ExerciseType::getGroupId));
        for (ExerciseType global : globalTypes) {
            if ("未运动惩罚".equals(global.getName())) continue;
            ExerciseType copy = new ExerciseType();
            copy.setName(global.getName());
            copy.setUnit(global.getUnit());
            copy.setMaleCoefficient(global.getMaleCoefficient());
            copy.setFemaleCoefficient(global.getFemaleCoefficient());
            copy.setDailyCap(global.getDailyCap());
            copy.setSortOrder(global.getSortOrder());
            copy.setActive(global.getActive());
            copy.setGroupId(group.getId());
            exerciseTypeMapper.insert(copy);
        }

        // 将创建者旧记录的运动类型迁移到小组副本（避免upsert时因ID不同产生重复记录）
        migrateUserRecordsToGroup(creatorUserId, group.getId());

        // 复制全局奖励到小组
        List<RewardItem> globalRewards = rewardItemMapper.selectList(new LambdaQueryWrapper<RewardItem>().isNull(RewardItem::getGroupId));
        for (RewardItem global : globalRewards) {
            RewardItem copy = new RewardItem();
            copy.setName(global.getName());
            copy.setDescription(global.getDescription());
            copy.setPointsCost(global.getPointsCost());
            copy.setMaxAmount(global.getMaxAmount());
            copy.setSortOrder(global.getSortOrder());
            copy.setGroupId(group.getId());
            rewardItemMapper.insert(copy);
        }

        return toGroupResponse(group, creatorUserId);
    }

    // 获取所有小组列表（含当前用户是否已加入、是否管理员信息）
    public List<GroupResponse> getAllGroups(Long currentUserId) {
        List<UserGroup> groups = groupMapper.selectList(null);
        return groups.stream()
                .map(g -> toGroupResponse(g, currentUserId))
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(Long groupId, Long currentUserId) {
        UserGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        return toGroupResponse(group, currentUserId);
    }

    // 获取小组成员列表（仅成员可查看）
    public List<GroupMemberResponse> getMembers(Long groupId, Long currentUserId) {
        UserGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("用户不存在");
        }

        if (!groupId.equals(currentUser.getGroupId())) {
            throw new BusinessException("您不是该小组成员");
        }

        List<User> members = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getGroupId, groupId));
        return members.stream()
                .map(u -> new GroupMemberResponse(
                        u.getId(), u.getName(),
                        u.getGender().name().toLowerCase(),
                        u.getId().equals(group.getCreatorId())
                                || Boolean.TRUE.equals(u.getIsGroupAdmin())))
                .collect(Collectors.toList());
    }

    // 申请加入小组：每人只能加入一个小组，不可重复申请
    @Transactional
    public void requestJoinGroup(Long groupId, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getGroupId() != null) {
            throw new BusinessException("您已在小组中，请先退出当前小组");
        }
        UserGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (joinRequestMapper.selectCount(new LambdaQueryWrapper<GroupJoinRequest>()
                .eq(GroupJoinRequest::getGroupId, groupId)
                .eq(GroupJoinRequest::getUserId, userId)
                .eq(GroupJoinRequest::getStatus, "pending")) > 0) {
            throw new BusinessException("您已提交过申请，请等待审批");
        }
        GroupJoinRequest req = new GroupJoinRequest();
        req.setGroupId(groupId);
        req.setUserId(userId);
        req.setStatus("pending");
        joinRequestMapper.insert(req);
    }

    // 获取待审批的加入申请（仅管理员可查看）
    public List<JoinRequestResponse> getPendingRequests(Long groupId, Long adminUserId) {
        if (!isGroupAdmin(adminUserId, groupId)) {
            throw new BusinessException("仅管理员可查看申请");
        }
        List<GroupJoinRequest> requests = joinRequestMapper.selectList(
                new LambdaQueryWrapper<GroupJoinRequest>()
                        .eq(GroupJoinRequest::getGroupId, groupId)
                        .eq(GroupJoinRequest::getStatus, "pending")
                        .orderByDesc(GroupJoinRequest::getCreatedAt));
        return requests.stream().map(r -> {
            User u = userMapper.selectById(r.getUserId());
            UserGroup g = groupMapper.selectById(r.getGroupId());
            return new JoinRequestResponse(
                    r.getId(), r.getGroupId(),
                    g != null ? g.getName() : "",
                    r.getUserId(),
                    u != null ? u.getName() : "",
                    r.getStatus(),
                    r.getCreatedAt() != null ? r.getCreatedAt().toString() : null
            );
        }).collect(Collectors.toList());
    }

    // 获取当前用户的待审批申请
    public List<JoinRequestResponse> getMyPendingRequests(Long userId) {
        List<GroupJoinRequest> requests = joinRequestMapper.selectList(
                new LambdaQueryWrapper<GroupJoinRequest>()
                        .eq(GroupJoinRequest::getUserId, userId)
                        .eq(GroupJoinRequest::getStatus, "pending"));
        return requests.stream().map(r -> {
            UserGroup g = groupMapper.selectById(r.getGroupId());
            return new JoinRequestResponse(
                    r.getId(), r.getGroupId(),
                    g != null ? g.getName() : "",
                    r.getUserId(), "", r.getStatus(),
                    r.getCreatedAt() != null ? r.getCreatedAt().toString() : null
            );
        }).collect(Collectors.toList());
    }

    // 审批通过：设置用户groupId和权限，同时拒绝该用户的其他待审批申请
    @Transactional
    public void approveRequest(Long requestId, Long adminUserId) {
        GroupJoinRequest req = joinRequestMapper.selectById(requestId);
        if (req == null) {
            throw new BusinessException("申请不存在");
        }
        if (!"pending".equals(req.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        if (!isGroupAdmin(adminUserId, req.getGroupId())) {
            throw new BusinessException("仅管理员可审批");
        }
        User user = userMapper.selectById(req.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getGroupId() != null) {
            throw new BusinessException("该用户已加入其他小组");
        }
        req.setStatus("approved");
        req.setProcessedBy(adminUserId);
        req.setProcessedAt(java.time.LocalDateTime.now());
        joinRequestMapper.updateById(req);

        user.setGroupId(req.getGroupId());
        userMapper.updateById(user);

        // 迁移用户旧记录到小组运动类型
        migrateUserRecordsToGroup(user.getId(), req.getGroupId());
    }

    // 审批拒绝
    @Transactional
    public void rejectRequest(Long requestId, Long adminUserId) {
        GroupJoinRequest req = joinRequestMapper.selectById(requestId);
        if (req == null) {
            throw new BusinessException("申请不存在");
        }
        if (!"pending".equals(req.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        if (!isGroupAdmin(adminUserId, req.getGroupId())) {
            throw new BusinessException("仅管理员可审批");
        }
        req.setStatus("rejected");
        req.setProcessedBy(adminUserId);
        req.setProcessedAt(java.time.LocalDateTime.now());
        joinRequestMapper.updateById(req);
    }

    // 退出小组：创建者不允许退出（只能解散）
    @Transactional
    public void leaveGroup(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getGroupId() == null) {
            throw new BusinessException("您未加入任何小组");
        }
        UserGroup group = groupMapper.selectById(user.getGroupId());
        if (group == null) {
            throw new BusinessException("小组不存在");
        }

        if (userId.equals(group.getCreatorId())) {
            throw new BusinessException("小组创建者无法退出，请先解散小组");
        }

        // 退出前将该成员的运动记录迁回全局运动类型
        migrateUserRecordsToGlobal(userId, user.getGroupId());

        // 用 UpdateWrapper 显式 set null（MyBatis-Plus updateById 默认忽略 null 字段）
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getGroupId, null)
                .set(User::getIsGroupAdmin, false));
    }

    // 解散小组：仅创建者可操作，清除所有成员的groupId和权限
    @Transactional
    public void dissolveGroup(Long groupId, Long userId) {
        UserGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        if (!userId.equals(group.getCreatorId())) {
            throw new BusinessException("仅小组创建者可解散小组");
        }

        // 清除所有成员的 groupId 和管理员标记（用 UpdateWrapper 显式 set null）
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getGroupId, groupId)
                .set(User::getGroupId, null)
                .set(User::getIsGroupAdmin, false));

        // 删除运动类型前，将所有成员的运动记录迁回全局运动类型
        migrateGroupRecordsToGlobal(groupId);

        // 删除小组专属运动类型
        List<ExerciseType> groupTypes = exerciseTypeMapper.selectList(
                new LambdaQueryWrapper<ExerciseType>().eq(ExerciseType::getGroupId, groupId).orderByAsc(ExerciseType::getSortOrder));
        for (ExerciseType t : groupTypes) {
            exerciseTypeMapper.deleteById(t.getId());
        }

        // 删除小组专属奖励
        List<RewardItem> groupRewards = rewardItemMapper.selectList(
                new LambdaQueryWrapper<RewardItem>().eq(RewardItem::getGroupId, groupId).orderByAsc(RewardItem::getSortOrder));
        for (RewardItem r : groupRewards) {
            rewardItemMapper.deleteById(r.getId());
        }

        groupMapper.deleteById(groupId);
    }

    // 判断是否为小组管理员：isGroupAdmin字段 或 创建者双重判定
    public boolean isGroupAdmin(Long userId, Long groupId) {
        if (groupId == null) return false;
        User user = userMapper.selectById(userId);
        if (user == null || !groupId.equals(user.getGroupId())) return false;
        // 创建者永远是管理员，或者被设为管理员的成员
        UserGroup group = groupMapper.selectById(groupId);
        return group != null && (userId.equals(group.getCreatorId())
                || Boolean.TRUE.equals(user.getIsGroupAdmin()));
    }

    // 设置/取消成员管理员角色：创建者不可被取消，至少保留一个管理员
    @Transactional
    public void setMemberAdmin(Long groupId, Long targetUserId, Long currentUserId) {
        if (!isGroupAdmin(currentUserId, groupId)) {
            throw new BusinessException("仅小组管理员可设置管理员");
        }
        UserGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("小组不存在");
        }
        // 创建者的管理员身份不可取消
        if (targetUserId.equals(group.getCreatorId())) {
            throw new BusinessException("创建者管理员身份不可更改");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException("目标用户不存在");
        }
        if (!groupId.equals(target.getGroupId())) {
            throw new BusinessException("该用户不是本小组成员");
        }
        // 切换管理员状态
        boolean currentAdmin = Boolean.TRUE.equals(target.getIsGroupAdmin());
        if (currentAdmin) {
            // 取消管理员：检查是否是唯一的管理员（不含创建者）
            long adminCount = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getGroupId, groupId)).stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsGroupAdmin())).count();
            if (adminCount <= 1) {
                throw new BusinessException("不能取消最后一个管理员");
            }
        }
        target.setIsGroupAdmin(!currentAdmin);
        userMapper.updateById(target);
    }

    public Long getUserGroupId(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getGroupId() : null;
    }

    public List<Long> getGroupMemberIds(Long groupId) {
        if (groupId == null) return Collections.emptyList();
        return userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getGroupId, groupId)).stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }

    private GroupResponse toGroupResponse(UserGroup group, Long currentUserId) {
        int memberCount = userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getGroupId, group.getId())).size();
        User creator = userMapper.selectById(group.getCreatorId());
        String creatorName = creator != null ? creator.getName() : "";
        User currentUser = userMapper.selectById(currentUserId);
        Long myGroupId = currentUser != null ? currentUser.getGroupId() : null;
        boolean amAdmin = currentUser != null && group.getId() != null
                && group.getId().equals(myGroupId) && Boolean.TRUE.equals(currentUser.getIsGroupAdmin());

        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getCreatorId(),
                creatorName,
                memberCount,
                group.getId().equals(myGroupId),
                amAdmin,
                group.getCreatedAt() != null ? group.getCreatedAt().toString() : null
        );
    }

    // 将用户旧运动记录中的全局运动类型ID替换为小组副本ID，避免upsert时产生重复记录
    private void migrateUserRecordsToGroup(Long userId, Long groupId) {
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>().eq(ExerciseRecord::getUserId, userId));
        if (records.isEmpty()) return;

        // 小组类型：名称 → ID
        List<ExerciseType> groupTypes = exerciseTypeMapper.selectList(
                new LambdaQueryWrapper<ExerciseType>().eq(ExerciseType::getGroupId, groupId));
        java.util.Map<String, Long> nameToId = new java.util.HashMap<>();
        for (ExerciseType gt : groupTypes) {
            nameToId.put(gt.getName(), gt.getId());
        }

        // 全局类型：ID → 名称
        List<ExerciseType> globalTypes = exerciseTypeMapper.selectList(
                new LambdaQueryWrapper<ExerciseType>().isNull(ExerciseType::getGroupId));
        java.util.Map<Long, String> idToName = new java.util.HashMap<>();
        for (ExerciseType gt : globalTypes) {
            idToName.put(gt.getId(), gt.getName());
        }

        for (ExerciseRecord record : records) {
            String typeName = idToName.get(record.getExerciseTypeId());
            if (typeName != null && nameToId.containsKey(typeName)) {
                record.setExerciseTypeId(nameToId.get(typeName));
                exerciseRecordMapper.updateById(record);
            }
        }
    }

    // 将小组内所有成员的运动记录迁回全局运动类型（解散小组前调用）
    private void migrateGroupRecordsToGlobal(Long groupId) {
        // 小组类型：ID → 名称
        List<ExerciseType> groupTypes = exerciseTypeMapper.selectList(
                new LambdaQueryWrapper<ExerciseType>().eq(ExerciseType::getGroupId, groupId));
        if (groupTypes.isEmpty()) return;

        java.util.Map<Long, String> groupTypeIdToName = new java.util.HashMap<>();
        List<Long> groupTypeIds = new java.util.ArrayList<>();
        for (ExerciseType gt : groupTypes) {
            groupTypeIdToName.put(gt.getId(), gt.getName());
            groupTypeIds.add(gt.getId());
        }

        // 全局类型：名称 → ID
        List<ExerciseType> globalTypes = exerciseTypeMapper.selectList(
                new LambdaQueryWrapper<ExerciseType>().isNull(ExerciseType::getGroupId));
        java.util.Map<String, Long> nameToGlobalId = new java.util.HashMap<>();
        for (ExerciseType gt : globalTypes) {
            nameToGlobalId.put(gt.getName(), gt.getId());
        }

        // 查找所有引用小组运动类型的记录
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>().in(ExerciseRecord::getExerciseTypeId, groupTypeIds));
        for (ExerciseRecord record : records) {
            String typeName = groupTypeIdToName.get(record.getExerciseTypeId());
            if (typeName != null && nameToGlobalId.containsKey(typeName)) {
                record.setExerciseTypeId(nameToGlobalId.get(typeName));
                exerciseRecordMapper.updateById(record);
            }
        }
    }

    // 将单个用户的运动记录从小组类型迁回全局类型（退出小组时调用）
    private void migrateUserRecordsToGlobal(Long userId, Long groupId) {
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>().eq(ExerciseRecord::getUserId, userId));
        if (records.isEmpty()) return;

        // 小组类型：ID → 名称
        List<ExerciseType> groupTypes = exerciseTypeMapper.selectList(
                new LambdaQueryWrapper<ExerciseType>().eq(ExerciseType::getGroupId, groupId));
        java.util.Map<Long, String> groupTypeIdToName = new java.util.HashMap<>();
        for (ExerciseType gt : groupTypes) {
            groupTypeIdToName.put(gt.getId(), gt.getName());
        }

        // 全局类型：名称 → ID
        List<ExerciseType> globalTypes = exerciseTypeMapper.selectList(
                new LambdaQueryWrapper<ExerciseType>().isNull(ExerciseType::getGroupId));
        java.util.Map<String, Long> nameToGlobalId = new java.util.HashMap<>();
        for (ExerciseType gt : globalTypes) {
            nameToGlobalId.put(gt.getName(), gt.getId());
        }

        for (ExerciseRecord record : records) {
            String typeName = groupTypeIdToName.get(record.getExerciseTypeId());
            if (typeName != null && nameToGlobalId.containsKey(typeName)) {
                record.setExerciseTypeId(nameToGlobalId.get(typeName));
                exerciseRecordMapper.updateById(record);
            }
        }
    }
}
