package org.example.sportplan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.dto.request.CreateExerciseTypeRequest;
import org.example.sportplan.dto.response.ExerciseTypeResponse;
import org.example.sportplan.entity.ExerciseType;
import org.example.sportplan.exception.BusinessException;
import org.example.sportplan.mapper.ExerciseTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 运动类型服务层
 * 处理运动类型的业务逻辑，包括查询、创建、更新和删除运动类型。
 * 仅管理员有权限创建、更新和删除运动类型。
 * 运动类型定义了每种运动的名称、单位、性别差异化积分系数和每日积分上限。
 */
@Service
@RequiredArgsConstructor
public class ExerciseTypeService {

    private final ExerciseTypeMapper exerciseTypeMapper;

    /**
     * 获取所有启用中的运动类型列表
     * 供用户端使用，不展示已停用的运动类型。
     *
     * @return 启用中的运动类型响应列表，按排序序号升序
     */
    public List<ExerciseTypeResponse> getActive(Long groupId) {
        if (groupId != null) {
            return exerciseTypeMapper.selectList(new LambdaQueryWrapper<ExerciseType>()
                    .eq(ExerciseType::getGroupId, groupId)
                    .eq(ExerciseType::getActive, true)
                    .orderByAsc(ExerciseType::getSortOrder)).stream()
                    .map(this::toResponse).collect(Collectors.toList());
        }
        return exerciseTypeMapper.selectList(new LambdaQueryWrapper<ExerciseType>()
                .eq(ExerciseType::getActive, true)
                .isNull(ExerciseType::getGroupId)
                .orderByAsc(ExerciseType::getSortOrder)).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 获取所有运动类型列表（含已停用）
     */
    public List<ExerciseTypeResponse> getAll(Long groupId) {
        if (groupId != null) {
            return exerciseTypeMapper.selectList(new LambdaQueryWrapper<ExerciseType>()
                    .eq(ExerciseType::getGroupId, groupId)
                    .orderByAsc(ExerciseType::getSortOrder)).stream()
                    .map(this::toResponse).collect(Collectors.toList());
        }
        return exerciseTypeMapper.selectList(new LambdaQueryWrapper<ExerciseType>()
                .isNull(ExerciseType::getGroupId)
                .orderByAsc(ExerciseType::getSortOrder)).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * 创建新的运动类型
     * 设置各项属性，未指定的可选字段使用默认值。
     *
     * @param request 创建请求，包含名称、单位、性别系数等
     * @return 创建成功的运动类型响应
     */
    @Transactional
    public ExerciseTypeResponse create(Long groupId, CreateExerciseTypeRequest request) {
        ExerciseType type = new ExerciseType();
        type.setGroupId(groupId);
        type.setName(request.getName());
        type.setUnit(request.getUnit());
        type.setMaleCoefficient(request.getMaleCoefficient());
        type.setFemaleCoefficient(request.getFemaleCoefficient());
        // 未指定每日上限时默认10分
        type.setDailyCap(request.getDailyCap() != null ? request.getDailyCap() : 10);
        // 未指定排序时默认为0
        type.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        // 未指定启用状态时默认启用
        type.setActive(request.getActive() != null ? request.getActive() : true);
        exerciseTypeMapper.insert(type);
        return toResponse(type);
    }

    /**
     * 更新运动类型
     * 仅更新请求中提供的非空字段，未提供的字段保持原值不变。
     *
     * @param id      运动类型ID
     * @param request 更新请求
     * @return 更新后的运动类型响应
     * @throws BusinessException 当运动类型不存在时抛出
     */
    @Transactional
    public ExerciseTypeResponse update(Long id, CreateExerciseTypeRequest request) {
        ExerciseType type = exerciseTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException("运动类型不存在");
        }
        type.setName(request.getName());
        type.setUnit(request.getUnit());
        type.setMaleCoefficient(request.getMaleCoefficient());
        type.setFemaleCoefficient(request.getFemaleCoefficient());
        // 仅在请求中明确提供了值时才更新可选字段
        if (request.getDailyCap() != null) type.setDailyCap(request.getDailyCap());
        if (request.getSortOrder() != null) type.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) type.setActive(request.getActive());
        exerciseTypeMapper.updateById(type);
        return toResponse(type);
    }

    /**
     * 删除运动类型
     * 物理删除，直接从数据库中移除记录。
     *
     * @param id 运动类型ID
     * @throws BusinessException 当运动类型不存在时抛出
     */
    @Transactional
    public void delete(Long id) {
        if (exerciseTypeMapper.selectById(id) == null) {
            throw new BusinessException("运动类型不存在");
        }
        exerciseTypeMapper.deleteById(id);
    }

    /**
     * 根据ID获取运动类型实体
     * 供其他服务层内部调用（如 ExerciseRecordService 计算积分时需要获取系数）。
     *
     * @param id 运动类型ID
     * @return 运动类型实体
     * @throws BusinessException 当运动类型不存在时抛出
     */
    public ExerciseType getById(Long id) {
        ExerciseType type = exerciseTypeMapper.selectById(id);
        if (type == null) {
            throw new BusinessException("运动类型不存在");
        }
        return type;
    }

    /**
     * 将运动类型实体转换为响应 DTO
     *
     * @param t 运动类型实体
     * @return 运动类型响应 DTO
     */
    private ExerciseTypeResponse toResponse(ExerciseType t) {
        return new ExerciseTypeResponse(t.getId(), t.getName(), t.getUnit(),
                t.getMaleCoefficient(), t.getFemaleCoefficient(),
                t.getDailyCap(), t.getSortOrder(), t.getActive(), t.getGroupId());
    }
}
