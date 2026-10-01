package com.tailoredplatform.ecommerce.inventory.entity;

import com.tailoredplatform.ecommerce.common.BaseEntity;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * quantityAvailable is what shoppers see as in-stock. quantityReserved is
 * stock held by open carts / pending orders so two concurrent checkouts
 * can't both claim the last unit. The @Version column makes every mutation
 * (reserve, release, commit) optimistically locked — see InventoryService.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Inventory extends BaseEntity {

    @EqualsAndHashCode.Include
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false, unique = true)
    private ProductVariant productVariant;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable = 0;

    @Column(name = "quantity_reserved", nullable = false)
    private Integer quantityReserved = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    private Integer lowStockThreshold = 5;

    public int sellable() {
        return quantityAvailable - quantityReserved;
    }

    public boolean isLowStock() {
        return sellable() <= lowStockThreshold;
    }
}
