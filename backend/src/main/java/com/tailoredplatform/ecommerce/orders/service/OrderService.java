package com.tailoredplatform.ecommerce.orders.service;

import com.tailoredplatform.ecommerce.admin.entity.AuditLog;
import com.tailoredplatform.ecommerce.admin.repository.AuditLogRepository;
import com.tailoredplatform.ecommerce.cart.entity.Cart;
import com.tailoredplatform.ecommerce.cart.entity.CartItem;
import com.tailoredplatform.ecommerce.cart.repository.CartItemRepository;
import com.tailoredplatform.ecommerce.cart.repository.CartRepository;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.coupons.dto.CouponValidationResult;
import com.tailoredplatform.ecommerce.coupons.service.CouponService;
import com.tailoredplatform.ecommerce.inventory.service.InventoryService;
import com.tailoredplatform.ecommerce.notifications.entity.Notification;
import com.tailoredplatform.ecommerce.notifications.service.EmailService;
import com.tailoredplatform.ecommerce.notifications.service.NotificationService;
import com.tailoredplatform.ecommerce.orders.dto.CheckoutRequest;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.entity.OrderItem;
import com.tailoredplatform.ecommerce.orders.repository.OrderRepository;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import com.tailoredplatform.ecommerce.shipping.service.ShipmentService;
import com.tailoredplatform.ecommerce.users.entity.Address;
import com.tailoredplatform.ecommerce.users.entity.User;
import com.tailoredplatform.ecommerce.users.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Order lifecycle and its relationship to inventory:
 *
 *   checkout()                -> Order PENDING,   inventory RESERVED (not yet sold)
 *   confirmPayment()          -> Order CONFIRMED, reservation COMMITTED as a sale
 *   releaseForFailedPayment() -> Order CANCELLED, reservation RELEASED
 *   cancel() while PENDING    -> reservation RELEASED (payment never completed)
 *   cancel() while CONFIRMED  -> sale REVERSED (stock returned) + refund triggered
 *
 * Every branch is annotated at its call site below with which of these it is.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("300");
    private static final BigDecimal FLAT_SHIPPING_FEE = new BigDecimal("15.00");

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final AuditLogRepository auditLogRepository;

    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final EmailService emailService;
    private final ShipmentService shipmentService;
    private final NotificationService notificationService;

    @Transactional
    public Order checkout(User user, CheckoutRequest request) {
        // Idempotency: a repeated request with the same key (double-click,
        // network retry) returns the already-created order instead of
        // placing a second one.
        var existing = orderRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessRuleViolationException("Cart is empty."));
        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleViolationException("Cannot check out an empty cart.");
        }

        Address shippingAddress = requireOwnedAddress(user, request.shippingAddressId());
        Address billingAddress = requireOwnedAddress(user, request.billingAddressId());

        // Server-side price recalculation: every line item's price is taken
        // from ProductVariant.effectivePrice() RIGHT NOW, never from the
        // cart's unitPriceSnapshot — this is what makes client-side price
        // manipulation impossible.
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems()) {
            ProductVariant variant = cartItem.getProductVariant();
            BigDecimal unitPrice = variant.effectivePrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            subtotal = subtotal.add(lineTotal);

            OrderItem item = new OrderItem();
            item.setProductVariant(variant);
            item.setProductNameSnapshot(variant.getProduct().getName());
            item.setSkuSnapshot(variant.getSku());
            item.setVariantLabelSnapshot(buildVariantLabel(variant));
            item.setUnitPriceSnapshot(unitPrice);
            item.setQuantity(cartItem.getQuantity());
            item.setLineTotal(lineTotal);
            orderItems.add(item);
        }

        // Reserve stock for every line BEFORE creating the order. If any one
        // line is short, the whole checkout fails and nothing already
        // reserved is left dangling — @Transactional rolls the earlier
        // reserve() calls back along with everything else.
        for (OrderItem item : orderItems) {
            inventoryService.reserve(item.getProductVariant().getId(), item.getQuantity(), "ORDER_PENDING", null);
        }

        BigDecimal discountTotal = BigDecimal.ZERO;
        CouponValidationResult couponResult = null;
        if (request.couponCode() != null && !request.couponCode().isBlank()) {
            couponResult = couponService.validate(request.couponCode(), user, subtotal);
            discountTotal = couponResult.discountAmount();
        }

        BigDecimal discountedSubtotal = subtotal.subtract(discountTotal);
        BigDecimal shippingFee = discountedSubtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO : FLAT_SHIPPING_FEE;
        BigDecimal taxTotal = discountedSubtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = discountedSubtotal.add(shippingFee).add(taxTotal);

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUser(user);
        order.setStatus(Order.Status.PENDING);
        order.setSubtotal(subtotal);
        order.setDiscountTotal(discountTotal);
        order.setShippingFee(shippingFee);
        order.setTaxTotal(taxTotal);
        order.setGrandTotal(grandTotal);
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        order.setIdempotencyKey(request.idempotencyKey());
        order.setPlacedAt(Instant.now());
        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        if (couponResult != null) {
            order.setCoupon(couponResult.coupon());
        }

        orderRepository.save(order);

        // Note: the RESERVATION movements written above (inside the reserve()
        // loop) are logged with referenceId=null, since the order doesn't
        // have an id yet at that point — a known simplification documented
        // in the README rather than left silent; SALE/RELEASE movements
        // later in the lifecycle DO carry the order id correctly.

        if (couponResult != null) {
            couponService.recordRedemption(couponResult.coupon(), user, order, discountTotal);
        }

        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();

        return order;
    }

    @Transactional
    public void confirmPayment(Long orderId) {
        Order order = requireOrder(orderId);
        if (!order.canTransitionTo(Order.Status.CONFIRMED)) {
            throw new BusinessRuleViolationException(
                    "Cannot confirm order in status " + order.getStatus());
        }

        for (OrderItem item : order.getItems()) {
            inventoryService.commitSale(item.getProductVariant().getId(), item.getQuantity(), "ORDER", order.getId());
        }

        order.setStatus(Order.Status.CONFIRMED);
        orderRepository.save(order);

        emailService.sendOrderConfirmationEmail(order.getUser().getEmail(), order.getUser().getFullName(), order.getOrderNumber());
        notificationService.create(order.getUser(), Notification.Type.ORDER_CONFIRMED,
                "Order confirmed", "Your order " + order.getOrderNumber() + " has been confirmed.",
                "ORDER", order.getId());
        writeAudit(order.getUser(), "ORDER_CONFIRMED", order);
    }

    @Transactional
    public void releaseForFailedPayment(Long orderId, String reason) {
        Order order = requireOrder(orderId);
        if (order.getStatus() != Order.Status.PENDING) {
            return; // already resolved by another event — nothing to release
        }

        for (OrderItem item : order.getItems()) {
            inventoryService.release(item.getProductVariant().getId(), item.getQuantity(), "ORDER", order.getId());
        }

        order.setStatus(Order.Status.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancellationReason(reason);
        orderRepository.save(order);
    }

    @Transactional
    public Order cancel(User user, Long orderId, String reason) {
        Order order = requireOwnedOrder(user, orderId);

        if (!order.isCancellable()) {
            throw new BusinessRuleViolationException("This order can no longer be cancelled.");
        }

        boolean wasPaid = order.getStatus() == Order.Status.CONFIRMED;

        for (OrderItem item : order.getItems()) {
            if (wasPaid) {
                // Payment had already succeeded and inventory was committed
                // as a sale — reverse it by returning stock to available.
                inventoryService.returnStock(item.getProductVariant().getId(), item.getQuantity(), "ORDER_CANCELLED", order.getId());
            } else {
                // Still PENDING — nothing was ever sold, just release the hold.
                inventoryService.release(item.getProductVariant().getId(), item.getQuantity(), "ORDER_CANCELLED", order.getId());
            }
        }

        order.setStatus(Order.Status.CANCELLED);
        order.setCancelledAt(Instant.now());
        order.setCancellationReason(reason);
        orderRepository.save(order);

        writeAudit(user, "ORDER_CANCELLED" + (wasPaid ? "_REFUND_REQUIRED" : ""), order);
        notificationService.create(user, Notification.Type.ORDER_CANCELLED,
                "Order cancelled", "Your order " + order.getOrderNumber() + " has been cancelled.",
                "ORDER", order.getId());
        // Actual Stripe refund for the wasPaid case is issued separately by
        // PaymentService (see payments module) — cancellation and refund are
        // deliberately decoupled so a refund-gateway failure doesn't block
        // the order-state change the customer is waiting on.

        return order;
    }

    @Transactional
    public Order adminUpdateStatus(User admin, Long orderId, Order.Status newStatus, String note) {
        Order order = requireOrder(orderId);

        if (!order.canTransitionTo(newStatus)) {
            throw new BusinessRuleViolationException(
                    "Cannot move order from " + order.getStatus() + " to " + newStatus + ".");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        if (newStatus == Order.Status.SHIPPED) {
            shipmentService.markShipped(order, "TBD Carrier", "TBD-" + order.getOrderNumber());
            notificationService.create(order.getUser(), Notification.Type.ORDER_SHIPPED,
                    "Order shipped", "Your order " + order.getOrderNumber() + " is on its way.",
                    "ORDER", order.getId());
        } else if (newStatus == Order.Status.DELIVERED) {
            shipmentService.markDelivered(order);
            notificationService.create(order.getUser(), Notification.Type.ORDER_DELIVERED,
                    "Order delivered", "Your order " + order.getOrderNumber() + " has been delivered.",
                    "ORDER", order.getId());
        }

        writeAudit(admin, "ORDER_STATUS_CHANGED_TO_" + newStatus, order);
        return order;
    }

    @Transactional(readOnly = true)
    public Order getOwnedOrder(User user, Long orderId) {
        return requireOwnedOrder(user, orderId);
    }

    @Transactional(readOnly = true)
    public Order getOrderByNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderNumber));
    }

    @Transactional(readOnly = true)
    public Page<Order> listForUser(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable);
    }

    // ---- helpers ----

    private Order requireOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", orderId));
    }

    private Order requireOwnedOrder(User user, Long orderId) {
        Order order = requireOrder(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return order;
    }

    private Address requireOwnedAddress(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> ResourceNotFoundException.of("Address", addressId));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleViolationException("Address does not belong to the current user.");
        }
        return address;
    }

    private String buildVariantLabel(ProductVariant variant) {
        StringBuilder sb = new StringBuilder();
        if (variant.getColor() != null) sb.append(variant.getColor());
        if (variant.getSize() != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(variant.getSize());
        }
        return sb.toString();
    }

    private String generateOrderNumber() {
        String random = Long.toString(Math.abs(new SecureRandom().nextLong()), 36).toUpperCase();
        String suffix = random.length() > 6 ? random.substring(0, 6) : random;
        return "ORD-" + Instant.now().getEpochSecond() + "-" + suffix;
    }

    private void writeAudit(User actor, String action, Order order) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setEntityType("ORDER");
        log.setEntityId(order.getId());
        auditLogRepository.save(log);
    }
}
