package com.fixpoint.business.dashboard.dto;

import com.fixpoint.business.attachments.dto.AttachmentDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDTO {
    private Map<String, Long> ticketsByStatus;
    private List<DashboardMonthlyCountDTO> ticketsByMonth;
    private List<DashboardMonthlyTotalDTO> clientGrowth;
    private List<DashboardPartUsageDTO> partsUsage;
    private List<DashboardTopClientDTO> topClients;
    private DashboardStorageDTO storage;
    private List<AttachmentDTO> recentFiles;
}
