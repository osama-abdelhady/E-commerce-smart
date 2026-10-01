package com.tailoredplatform.ecommerce.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        BigDecimal totalSales,
        long totalOrders,
        long totalCustomers,
        long totalProducts,
        long lowStockCount,
        long pendingOrdersCount,
        List<RecentOrder> recentOrders,
        List<BestSeller> bestSellers
) {
    public record RecentOrder(String orderNumber, String customerEmail, String status, BigDecimal grandTotal) {}
    public record BestSeller(String productName, int reviewCount, BigDecimal averageRating) {}
}
