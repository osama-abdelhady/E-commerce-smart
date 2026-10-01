package com.tailoredplatform.ecommerce.inventory.repository;

import com.tailoredplatform.ecommerce.inventory.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId);
}
