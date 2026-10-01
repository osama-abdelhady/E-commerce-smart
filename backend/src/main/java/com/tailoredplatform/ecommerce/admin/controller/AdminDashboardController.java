package com.tailoredplatform.ecommerce.admin.controller;

import com.tailoredplatform.ecommerce.admin.dto.DashboardSummaryResponse;
import com.tailoredplatform.ecommerce.admin.dto.OrderStatusReportResponse;
import com.tailoredplatform.ecommerce.admin.dto.SalesReportResponse;
import com.tailoredplatform.ecommerce.admin.service.AdminReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Dashboard & Reports", description = "Overview metrics and sales/order/inventory reports")
public class AdminDashboardController {

    private final AdminReportsService reportsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard overview: total sales, orders, customers, products, low-stock, pending orders, recent orders, best sellers")
    public DashboardSummaryResponse dashboard() {
        return reportsService.getDashboardSummary();
    }

    @GetMapping("/reports/sales")
    @Operation(summary = "Sales report grouped by day for a date range (defaults to the last 30 days)")
    public SalesReportResponse salesReport(
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate
    ) {
        Instant to = toDate != null ? toDate : Instant.now();
        Instant from = fromDate != null ? fromDate : to.minus(30, ChronoUnit.DAYS);
        return reportsService.getSalesReport(from, to);
    }

    @GetMapping("/reports/order-status")
    @Operation(summary = "Order counts grouped by status")
    public OrderStatusReportResponse orderStatusReport() {
        return reportsService.getOrderStatusReport();
    }
}
