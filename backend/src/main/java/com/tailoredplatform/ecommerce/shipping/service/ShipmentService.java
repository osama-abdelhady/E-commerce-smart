package com.tailoredplatform.ecommerce.shipping.service;

import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.shipping.entity.Shipment;
import com.tailoredplatform.ecommerce.shipping.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;

    @Transactional
    public Shipment markShipped(Order order, String carrier, String trackingNumber) {
        Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElseGet(() -> {
            Shipment s = new Shipment();
            s.setOrder(order);
            return s;
        });
        shipment.setCarrier(carrier);
        shipment.setTrackingNumber(trackingNumber);
        shipment.setStatus(Shipment.Status.IN_TRANSIT);
        shipment.setShippedAt(Instant.now());
        shipment.setEstimatedDeliveryAt(Instant.now().plus(5, ChronoUnit.DAYS));
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Shipment markDelivered(Order order) {
        Shipment shipment = shipmentRepository.findByOrderId(order.getId()).orElseGet(() -> {
            Shipment s = new Shipment();
            s.setOrder(order);
            return s;
        });
        shipment.setStatus(Shipment.Status.DELIVERED);
        shipment.setDeliveredAt(Instant.now());
        return shipmentRepository.save(shipment);
    }
}
