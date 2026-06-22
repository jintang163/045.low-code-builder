package com.lowcode.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.attendance.entity.LocationConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LocationConfigMapper extends BaseMapper<LocationConfig> {

    @Select("SELECT * FROM att_location_config WHERE app_id = #{appId} AND status = 1 AND deleted = 0 ORDER BY is_default DESC, sort_order, id")
    List<LocationConfig> selectByAppId(@Param("appId") Long appId);

    @Select("SELECT * FROM att_location_config WHERE app_id = #{appId} AND is_default = 1 AND status = 1 AND deleted = 0 LIMIT 1")
    LocationConfig selectDefaultLocation(@Param("appId") Long appId);
}
