package com.siso.backend.admin;

import java.util.List;

public record DashboardDto(
        long totalComments,
        long totalVotes,
        long pendingReports,
        long totalReports,
        long newAnonUsersLast24h,
        long activeAnonUsersLast24h,
        List<SourceStatDto> sourceStats) {
}
