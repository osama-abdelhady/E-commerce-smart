package com.tailoredplatform.ecommerce.orders;

import com.tailoredplatform.ecommerce.admin.repository.AuditLogRepository;
import com.tailoredplatform.ecommerce.cart.entity.Cart;
import com.tailoredplatform.ecommerce.cart.entity.CartItem;
import com.tailoredplatform.ecommerce.cart.repository.CartItemRepository;
import com.tailoredplatform.ecommerce.cart.repository.CartRepository;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.coupons.service.CouponService;
import com.tailoredplatform.ecommerce.inventory.service.InventoryService;
import com.tailoredplatform.ecommerce.notifications.service.EmailService;
import com.tailoredplatform.ecommerce.notifications.service.NotificationService;
import com.tailoredplatform.ecommerce.orders.dto.CheckoutRequest;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.entity.OrderItem;
import com.tailoredplatform.ecommerce.orders.repository.OrderRepository;
import com.tailoredplatform.ecommerce.orders.service.OrderService;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import com.tailoredplatform.ecommerce.shipping.service.ShipmentService;
import com.tailoredplatform.ecommerce.users.entity.Address;
import com.tailoredplatform.ecommerce.users.entity.User;
import com.tailoredplatform.ecommerce.users.repository.AddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private InventoryService inventoryService;
    @Mock private CouponService couponService;
    @Mock private EmailService emailService;
    @Mock private ShipmentService shipmentService;
    @Mock private NotificationService notificationService;

    private OrderService orderService;
    private User user;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository, cartRepository, cartItemRepository, addressRepository, auditLogRepository,
                inventoryService, couponService, emailService, shipmentService, notificationService
        );
        user = new User();
        user.setId(1L);
        user.setEmail("customer@example.com");
        user.setFullName("Test Customer");
    }

    @Test
    void checkout_returnsTheExistingOrder_whenTheIdempotencyKeyWasAlreadyUsed() {
        Order existing = new Order();
        existing.setId(99L);
        when(orderRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        var request = new CheckoutRequest(1L, 1L, null, "dup-key");
        Order result = orderService.checkout(user, request);

        assertThat(result).isSameAs(existing);
        // A duplicate request must NEVER touch the cart or reserve inventory again.
        verify(cartRepository, never()).findByUserId(anyLong());
        verify(inventoryService, never()).reserve(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void checkout_throws_whenCartIsEmpty() {
        when(orderRepository.findByIdempotencyKey("key1")).thenReturn(Optional.empty());
        Cart emptyCart = new Cart();
        emptyCart.setItems(List.of());
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(emptyCart));

        var request = new CheckoutRequest(1L, 1L, null, "key1");

        assertThatThrownBy(() -> orderService.checkout(user, request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void checkout_reservesInventoryForEveryLine_beforeCreatingTheOrder() {
        when(orderRepository.findByIdempotencyKey("key2")).thenReturn(Optional.empty());

        Product product = new Product();
        product.setName("Test Suit");

        ProductVariant variant = new ProductVariant();
        variant.setId(55L);
        variant.setProduct(product);
        variant.setSku("SKU-1");
        variant.setPrice(new java.math.BigDecimal("100.00"));

        CartItem item = new CartItem();
        item.setProductVariant(variant);
        item.setQuantity(2);

        Cart cart = new Cart();
        cart.setId(7L);
        cart.setItems(new ArrayList<>(List.of(item)));

        Address address = new Address();
        address.setId(3L);
        address.setUser(user);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(addressRepository.findById(3L)).thenReturn(Optional.of(address));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CheckoutRequest(3L, 3L, null, "key2");
        orderService.checkout(user, request);

        verify(inventoryService, times(1)).reserve(eq(55L), eq(2), anyString(), any());
    }

    @Test
    void confirmPayment_throws_whenOrderIsNotInAConfirmableStatus() {
        Order order = new Order();
        order.setId(5L);
        order.setStatus(Order.Status.CANCELLED); // terminal — cannot become CONFIRMED
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.confirmPayment(5L))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(inventoryService, never()).commitSale(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    void confirmPayment_commitsSaleForEveryItem_andMovesOrderToConfirmed() {
        Product product = new Product();
        ProductVariant variant = new ProductVariant();
        variant.setId(77L);
        variant.setProduct(product);

        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        item.setQuantity(3);

        Order order = new Order();
        order.setId(6L);
        order.setUser(user);
        order.setOrderNumber("ORD-TEST-1");
        order.setStatus(Order.Status.PENDING);
        order.setItems(List.of(item));

        when(orderRepository.findById(6L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.confirmPayment(6L);

        assertThat(order.getStatus()).isEqualTo(Order.Status.CONFIRMED);
        verify(inventoryService, times(1)).commitSale(eq(77L), eq(3), anyString(), eq(6L));
        verify(emailService, times(1)).sendOrderConfirmationEmail(eq("customer@example.com"), anyString(), eq("ORD-TEST-1"));
    }

    @Test
    void adminUpdateStatus_throws_forAnInvalidTransition() {
        Order order = new Order();
        order.setId(8L);
        order.setStatus(Order.Status.PENDING);
        when(orderRepository.findById(8L)).thenReturn(Optional.of(order));

        // PENDING can never jump straight to SHIPPED
        assertThatThrownBy(() -> orderService.adminUpdateStatus(user, 8L, Order.Status.SHIPPED, null))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(orderRepository, never()).save(any());
    }
}
