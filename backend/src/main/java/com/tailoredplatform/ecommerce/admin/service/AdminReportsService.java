package com.tailoredplatform.ecommerce.admin.service;

import com.tailoredplatform.ecommerce.admin.dto.DashboardSummaryResponse;
import com.tailoredplatform.ecommerce.admin.dto.OrderStatusReportResponse;
import com.tailoredplatform.ecommerce.admin.dto.SalesReportResponse;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.repository.OrderRepository;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.repository.ProductRepository;
import com.tailoredplatform.ecommerce.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All aggregation here happens in Java over repository results, not via SQL
 * GROUP BY. That's a deliberate scope call for this build, not an oversight:
 * fine for a demo/portfolio-scale dataset, but a catalog with real production
 * order volume should push totalSales/sales-by-period/order-status-counts
 * into native/JPQL aggregate queries instead of loading every order into
 * memory — flagged in the README's Phase 9 performance-pass notes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminReportsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public DashboardSummaryResponse getDashboardSummary() {
        List<Order> allOrders = orderRepository.findAll();

        BigDecimal totalSales = allOrders.stream()
                .filter(o -> o.getStatus() != Order.Status.CANCELLED)
                .map(Order::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = allOrders.stream().filter(o -> o.getStatus() == Order.Status.PENDING).count();

        long lowStockCount = inventoryRepository.findAll().stream()
                .filter(inv -> inv.isLowStock())
                .count();

        List<DashboardSummaryResponse.RecentOrder> recentOrders = allOrders.stream()
                .sorted((a, b) -> b.getPlacedAt().compareTo(a.getPlacedAt()))
                .limit(10)
                .map(o -> new DashboardSummaryResponse.RecentOrder(
                        o.getOrderNumber(), o.getUser().getEmail(), o.getStatus().name(), o.getGrandTotal()))
                .toList();

        List<DashboardSummaryResponse.BestSeller> bestSellers = productRepository
                .findTop8ByIsBestSellerTrueAndStatusOrderByReviewCountDesc(Product.Status.ACTIVE)
                .stream()
                .map(p -> new DashboardSummaryResponse.BestSeller(p.getName(), p.getReviewCount(), p.getAverageRating()))
                .toList();

        return new DashboardSummaryResponse(
                totalSales,
                allOrders.size(),
                userRepository.count(),
                productRepository.count(),
                lowStockCount,
                pendingCount,
                recentOrders,
                bestSellers
        );
    }

    /** Groups revenue by calendar day for the given window — a simple, dependency-free "sales over time" chart data source. */
    public SalesReportResponse getSalesReport(Instant from, Instant to) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != Order.Status.CANCELLED)
                .filter(o -> !o.getPlacedAt().isBefore(from) && !o.getPlacedAt().isAfter(to))
                .toList();

        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
        Map<String, List<Order>> byDay = new LinkedHashMap<>();
        for (Order o : orders) {
            byDay.computeIfAbsent(dayFormat.format(o.getPlacedAt()), k -> new ArrayList<>()).add(o);
        }

        List<SalesReportResponse.SalesPoint> points = byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SalesReportResponse.SalesPoint(
                        e.getKey(),
                        e.getValue().stream().map(Order::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                        e.getValue().size()
                ))
                .toList();

        BigDecimal total = orders.stream().map(Order::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesReportResponse(points, total, orders.size());
    }

    public OrderStatusReportResponse getOrderStatusReport() {
        Map<Order.Status, Long> counts = new EnumMap<>(Order.Status.class);
        for (Order.Status status : Order.Status.values()) counts.put(status, 0L);
        for (Order order : orderRepository.findAll()) {
            counts.merge(order.getStatus(), 1L, Long::sum);
        }
        Map<String, Long> byName = new LinkedHashMap<>();
        counts.forEach((status, count) -> byName.put(status.name(), count));
        return new OrderStatusReportResponse(byName);
    }
}
