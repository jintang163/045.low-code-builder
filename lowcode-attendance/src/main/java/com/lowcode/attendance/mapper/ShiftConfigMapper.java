package com.lowcode.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.attendance.entity.ShiftConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ShiftConfigMapper extends BaseMapper<ShiftConfig> {

    @Select("SELECT * FROM att_shift_config WHERE (app_id = #{appId} OR app_id = 0) AND status = 1 AND deleted = 0 ORDER BY sort_order, id")
    List<ShiftConfig> selectByAppId(@Param("appId") Long appId);
}
