package com.lowcode.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.model.entity.SqlMigration;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SqlMigrationMapper extends BaseMapper<SqlMigration> {
}
