package com.lowcode.attendance.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClockInDTO {
    private Long appId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String location;
}
