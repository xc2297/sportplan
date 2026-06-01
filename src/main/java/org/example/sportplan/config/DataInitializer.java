package org.example.sportplan.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.sportplan.entity.ExerciseRecord;
import org.example.sportplan.entity.ExerciseType;
import org.example.sportplan.entity.RewardItem;
import org.example.sportplan.mapper.ExerciseRecordMapper;
import org.example.sportplan.mapper.ExerciseTypeMapper;
import org.example.sportplan.mapper.RewardItemMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 数据初始化器：应用启动时检查并插入预设运动类型和奖励项，并修复孤儿运动记录
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RewardItemMapper rewardItemMapper;
    private final ExerciseTypeMapper exerciseTypeMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;

    @Override
    public void run(String... args) {
        initRewards();
        initExerciseTypes();
        initPenaltyType();
        repairOrphanedRecords();
    }

    private void initRewards() {
        if (rewardItemMapper.selectCount(null) == 0) {
            rewardItemMapper.insert(new RewardItem(null, "即时畅饮券", "兑换一杯奶茶/咖啡等饮品",
                    5, new BigDecimal("30"), 1, null, null, null));
            rewardItemMapper.insert(new RewardItem(null, "欢乐聚餐券", "用于一次朋友或家庭聚餐",
                    10, new BigDecimal("120"), 2, null, null, null));
            rewardItemMapper.insert(new RewardItem(null, "休闲娱乐券", "用于电影、下午茶、游乐项目等",
                    20, new BigDecimal("200"), 3, null, null, null));
            rewardItemMapper.insert(new RewardItem(null, "豪华大餐券", "用于一次高品质餐厅体验",
                    30, new BigDecimal("300"), 4, null, null, null));
        }
    }

    private void initExerciseTypes() {
        if (exerciseTypeMapper.selectCount(null) == 0) {
            exerciseTypeMapper.insert(new ExerciseType(null, "跑步", "公里",
                    new BigDecimal("0.5"), new BigDecimal("1.0"), 10, 1, true, null, null, null));
            exerciseTypeMapper.insert(new ExerciseType(null, "走路", "公里",
                    BigDecimal.ZERO, new BigDecimal("0.5"), 10, 2, true, null, null, null));
            exerciseTypeMapper.insert(new ExerciseType(null, "俯卧撑", "个",
                    new BigDecimal("0.1"), new BigDecimal("0.1"), 10, 3, true, null, null, null));
            exerciseTypeMapper.insert(new ExerciseType(null, "仰卧起坐", "个",
                    new BigDecimal("0.05"), new BigDecimal("0.1"), 10, 4, true, null, null, null));
        }
    }

    private void initPenaltyType() {
        ExerciseType penalty = exerciseTypeMapper.selectOne(
                new LambdaQueryWrapper<ExerciseType>().eq(ExerciseType::getName, "未运动惩罚"));
        if (penalty == null) {
            exerciseTypeMapper.insert(new ExerciseType(null, "未运动惩罚", "次",
                    BigDecimal.ZERO, BigDecimal.ZERO, 1, 99, false, null, null, null));
        }
    }

    // 修复孤儿运动记录：解散小组后记录引用了已删除的运动类型，通过积分系数反推回全局类型
    private void repairOrphanedRecords() {
        List<ExerciseType> allTypes = exerciseTypeMapper.selectList(null);
        Map<Long, ExerciseType> typeMap = allTypes.stream()
                .collect(Collectors.toMap(ExerciseType::getId, t -> t));

        // 全局类型列表（不含惩罚类型）
        List<ExerciseType> globalTypes = allTypes.stream()
                .filter(t -> t.getGroupId() == null && !"未运动惩罚".equals(t.getName()))
                .collect(Collectors.toList());

        List<ExerciseRecord> allRecords = exerciseRecordMapper.selectList(null);
        for (ExerciseRecord record : allRecords) {
            // 如果运动类型存在，跳过
            if (typeMap.containsKey(record.getExerciseTypeId())) continue;

            // 孤儿记录：通过 score/amount 反推运动类型
            if (record.getAmount().compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal coefficient = record.getScore().divide(record.getAmount(), 4, RoundingMode.HALF_UP);

            ExerciseType matched = null;
            for (ExerciseType gt : globalTypes) {
                // 尝试匹配男或女系数
                if (gt.getMaleCoefficient().compareTo(coefficient) == 0
                        || gt.getFemaleCoefficient().compareTo(coefficient) == 0) {
                    matched = gt;
                    break;
                }
            }

            if (matched != null) {
                record.setExerciseTypeId(matched.getId());
                exerciseRecordMapper.updateById(record);
            } else {
                // 无法匹配则删除该孤儿记录
                exerciseRecordMapper.deleteById(record.getId());
            }
        }
    }
}
