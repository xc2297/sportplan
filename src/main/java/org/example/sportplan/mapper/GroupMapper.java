package org.example.sportplan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.sportplan.entity.UserGroup;

@Mapper
public interface GroupMapper extends BaseMapper<UserGroup> {
}
