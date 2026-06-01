package org.example.sportplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.sportplan.entity.ExerciseRecord;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ExerciseRecordMapper extends BaseMapper<ExerciseRecord> {

    // 查询指定日期有运动记录的去重用户ID
    List<Long> selectActiveUserIdsByDate(@Param("date") LocalDate recordDate);

    // 按日期范围和用户ID集合查询
    List<ExerciseRecord> selectByDateBetweenAndUserIds(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("userIds") List<Long> userIds);

    // 按日期和用户ID集合查询
    List<ExerciseRecord> selectByDateAndUserIds(@Param("date") LocalDate date, @Param("userIds") List<Long> userIds);
}
