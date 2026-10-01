package com.tailoredplatform.ecommerce.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalesReportResponse(
        List<SalesPoint> points,
        BigDecimal totalRevenue,
        long totalOrders
) {
    public record SalesPoint(String periodLabel, BigDecimal revenue, long orderCount) {

    }
}
