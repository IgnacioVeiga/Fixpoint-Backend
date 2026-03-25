package com.fixpoint.business.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardTopClientDTO {
    private String name;
    private long ticketCount;
}
