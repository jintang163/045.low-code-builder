package com.lowcode.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lowcode.attendance.entity.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface LeaveRequestMapper extends BaseMapper<LeaveRequest> {

    @Select("SELECT * FROM att_leave_request WHERE app_id = #{appId} AND user_id = #{userId} AND deleted = 0 ORDER BY created_at DESC")
    List<LeaveRequest> selectByUser(@Param("appId") Long appId, @Param("userId") Long userId);

    @Select("SELECT * FROM att_leave_request WHERE app_id = #{appId} AND status = #{status} AND deleted = 0 ORDER BY created_at DESC")
    List<LeaveRequest> selectByAppAndStatus(@Param("appId") Long appId, @Param("status") String status);

    @Select("SELECT * FROM att_leave_request WHERE app_id = #{appId} AND user_id = #{userId} AND start_date <= #{endDate} AND end_date >= #{startDate} AND deleted = 0 AND status != 'CANCELLED'")
    List<LeaveRequest> selectOverlappingLeaves(@Param("appId") Long appId,
                                                 @Param("userId") Long userId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
}
