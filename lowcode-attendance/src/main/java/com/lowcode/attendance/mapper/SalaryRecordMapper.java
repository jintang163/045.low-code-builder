package com.lowcode.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.attendance.entity.SalaryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SalaryRecordMapper extends BaseMapper<SalaryRecord> {

    @Select("SELECT * FROM att_salary_record WHERE app_id = #{appId} AND salary_month = #{salaryMonth} AND deleted = 0 ORDER BY user_id")
    List<SalaryRecord> selectByMonth(@Param("appId") Long appId, @Param("salaryMonth") String salaryMonth);

    @Select("SELECT * FROM att_salary_record WHERE app_id = #{appId} AND user_id = #{userId} AND deleted = 0 ORDER BY salary_month DESC")
    List<SalaryRecord> selectByUser(@Param("appId") Long appId, @Param("userId") Long userId);
}
