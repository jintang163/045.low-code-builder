package com.lowcode.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.attendance.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {

    @Select("SELECT * FROM att_attendance_record WHERE app_id = #{appId} AND user_id = #{userId} AND attendance_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY attendance_date DESC")
    List<AttendanceRecord> selectByUserAndDateRange(@Param("appId") Long appId,
                                                      @Param("userId") Long userId,
                                                      @Param("startDate") LocalDate startDate,
                                                      @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM att_attendance_record WHERE app_id = #{appId} AND attendance_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY attendance_date DESC, user_id")
    List<AttendanceRecord> selectByAppAndDateRange(@Param("appId") Long appId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM att_attendance_record WHERE app_id = #{appId} AND user_id = #{userId} AND attendance_date = #{date} AND deleted = 0 LIMIT 1")
    AttendanceRecord selectByUserAndDate(@Param("appId") Long appId,
                                          @Param("userId") Long userId,
                                          @Param("date") LocalDate date);
}
