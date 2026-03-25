package com.fixpoint.business.dashboard.controller;

import com.fixpoint.business.dashboard.dto.DashboardSummaryDTO;
import com.fixpoint.business.dashboard.dto.DashboardStorageDTO;
import com.fixpoint.business.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryDTO getSummary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/storage")
    public DashboardStorageDTO getStorageSummary() {
        return dashboardService.getStorageSummary();
    }
}
