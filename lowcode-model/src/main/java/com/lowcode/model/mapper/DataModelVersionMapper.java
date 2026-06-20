package com.lowcode.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.model.entity.DataModelVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DataModelVersionMapper extends BaseMapper<DataModelVersion> {

    @Select("SELECT * FROM sys_model_version WHERE model_id = #{modelId} AND deleted = 0 ORDER BY version DESC")
    List<DataModelVersion> selectByModelId(@Param("modelId") Long modelId);

    @Select("SELECT * FROM sys_model_version WHERE model_id = #{modelId} AND deleted = 0 ORDER BY version DESC LIMIT 1")
    DataModelVersion selectLatestByModelId(@Param("modelId") Long modelId);

    @Select("SELECT * FROM sys_model_version WHERE model_id = #{modelId} AND version = #{version} AND deleted = 0 LIMIT 1")
    DataModelVersion selectByVersion(@Param("modelId") Long modelId, @Param("version") String version);
}
