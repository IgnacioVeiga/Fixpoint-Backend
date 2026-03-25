package com.fixpoint.business.dashboard.service;

import com.fixpoint.business.dashboard.dto.DashboardSummaryDTO;
import com.fixpoint.business.dashboard.dto.DashboardStorageDTO;

public interface DashboardService {
    DashboardSummaryDTO getSummary();
    DashboardStorageDTO getStorageSummary();
}
