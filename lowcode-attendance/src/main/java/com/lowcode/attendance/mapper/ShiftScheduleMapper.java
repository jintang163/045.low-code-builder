package com.lowcode.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.attendance.entity.ShiftSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ShiftScheduleMapper extends BaseMapper<ShiftSchedule> {

    @Select("SELECT * FROM att_shift_schedule WHERE app_id = #{appId} AND user_id = #{userId} AND shift_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY shift_date")
    List<ShiftSchedule> selectByUserAndDateRange(@Param("appId") Long appId,
                                                   @Param("userId") Long userId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM att_shift_schedule WHERE app_id = #{appId} AND shift_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY user_id, shift_date")
    List<ShiftSchedule> selectByAppAndDateRange(@Param("appId") Long appId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
