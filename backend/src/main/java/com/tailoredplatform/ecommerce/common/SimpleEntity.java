package com.tailoredplatform.ecommerce.common;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/** For simple line/reference tables (order_items, cart_items, product_images, ...) with just an id. */
@Getter
@Setter
@MappedSuperclass
public abstract class SimpleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
