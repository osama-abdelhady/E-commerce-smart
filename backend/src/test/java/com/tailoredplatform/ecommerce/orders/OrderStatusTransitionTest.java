package com.tailoredplatform.ecommerce.orders;

import com.tailoredplatform.ecommerce.orders.entity.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTransitionTest {

    @ParameterizedTest(name = "{0} -> {1} should be {2}")
    @CsvSource({
            "PENDING, CONFIRMED, true",
            "PENDING, CANCELLED, true",
            "PENDING, PROCESSING, false",
            "PENDING, SHIPPED, false",
            "CONFIRMED, PROCESSING, true",
            "CONFIRMED, CANCELLED, true",
            "CONFIRMED, SHIPPED, false",
            "PROCESSING, SHIPPED, true",
            "PROCESSING, CANCELLED, true",
            "PROCESSING, DELIVERED, false",
            "SHIPPED, DELIVERED, true",
            "SHIPPED, CANCELLED, false",
            "SHIPPED, PROCESSING, false",
            "DELIVERED, REFUNDED, true",
            "DELIVERED, CANCELLED, false",
            "CANCELLED, CONFIRMED, false",
            "CANCELLED, PENDING, false",
            "REFUNDED, DELIVERED, false",
    })
    void transitionsMatchTheDefinedGraph(Order.Status from, Order.Status to, boolean expected) {
        Order order = new Order();
        order.setStatus(from);

        assertThat(order.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    void terminalStatusesAllowNoForwardTransitions() {
        for (Order.Status terminal : new Order.Status[]{Order.Status.CANCELLED, Order.Status.REFUNDED}) {
            Order order = new Order();
            order.setStatus(terminal);
            for (Order.Status target : Order.Status.values()) {
                assertThat(order.canTransitionTo(target))
                        .as("%s should never transition to %s", terminal, target)
                        .isFalse();
            }
        }
    }

    @Test
    void isCancellableOnlyForPendingAndConfirmed() {
        assertThat(withStatus(Order.Status.PENDING).isCancellable()).isTrue();
        assertThat(withStatus(Order.Status.CONFIRMED).isCancellable()).isTrue();
        assertThat(withStatus(Order.Status.PROCESSING).isCancellable()).isFalse();
        assertThat(withStatus(Order.Status.SHIPPED).isCancellable()).isFalse();
        assertThat(withStatus(Order.Status.DELIVERED).isCancellable()).isFalse();
        assertThat(withStatus(Order.Status.CANCELLED).isCancellable()).isFalse();
        assertThat(withStatus(Order.Status.REFUNDED).isCancellable()).isFalse();
    }

    private Order withStatus(Order.Status status) {
        Order order = new Order();
        order.setStatus(status);
        return order;
    }
}
