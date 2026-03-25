package com.fixpoint.business.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStorageDTO {
    private long usedBytes;
    private long fileCount;
    private Long totalBytes;
    private Long availableBytes;
    private Double usagePercent;
    private String source;
}
