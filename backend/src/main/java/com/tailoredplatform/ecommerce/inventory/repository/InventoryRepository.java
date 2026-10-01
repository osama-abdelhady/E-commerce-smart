package com.tailoredplatform.ecommerce.inventory.repository;

import com.tailoredplatform.ecommerce.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductVariantId(Long productVariantId);

    /** Batch fetch for ProductMapper — avoids the N+1 pattern of one query per variant on a listing page. */
    List<Inventory> findByProductVariantIdIn(List<Long> productVariantIds);

    /**
     * PESSIMISTIC_WRITE row lock for the reserve/release/commit path in
     * InventoryService: under concurrent checkouts, the second transaction
     * blocks until the first commits, so sellable() is always read fresh —
     * the @Version column alone (optimistic) isn't enough for a hot SKU
     * where many customers race to buy the last unit.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.productVariant.id = :variantId")
    Optional<Inventory> findByProductVariantIdForUpdate(Long variantId);

    List<Inventory> findByQuantityAvailableLessThanEqual(Integer threshold);
}
