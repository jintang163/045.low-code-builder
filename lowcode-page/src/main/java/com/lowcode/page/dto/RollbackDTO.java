package com.lowcode.page.dto;

import lombok.Data;

@Data
public class RollbackDTO {

    private Long snapshotId;

    private String rollbackReason;

    private Boolean createNewSnapshot;
}
