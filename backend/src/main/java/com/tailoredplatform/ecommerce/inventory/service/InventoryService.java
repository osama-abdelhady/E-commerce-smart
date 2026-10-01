package com.tailoredplatform.ecommerce.inventory.service;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.inventory.entity.Inventory;
import com.tailoredplatform.ecommerce.inventory.entity.InventoryMovement;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryMovementRepository;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The reserve/release/commit lifecycle that makes checkout safe under
 * concurrent requests for the same SKU:
 *
 *   1. RESERVE  at "place order" — moves stock from available into
 *      reserved. Two customers racing for the last unit: the second one's
 *      transaction blocks on the PESSIMISTIC_WRITE row lock until the first
 *      commits or rolls back, then re-reads sellable() fresh and correctly
 *      fails if nothing is left.
 *   2. COMMIT   at "payment succeeded" — the reservation becomes a real
 *      sale: both available and reserved decrease together.
 *   3. RELEASE  at "order cancelled" / "payment failed" / "reservation
 *      expired" — reserved stock returns to available without changing
 *      quantity_available (the units were never actually sold).
 *
 * Every mutation writes an InventoryMovement row — never silently.
 * Propagation.MANDATORY on the core three methods is deliberate: reserve/
 * release/commit must never run outside a caller's transaction (OrderService,
 * PaymentService), since they need to be atomic with the order/payment state
 * change that triggered them.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(Long productVariantId, int quantity, String referenceType, Long referenceId) {
        Inventory inventory = lockInventory(productVariantId);

        if (inventory.sellable() < quantity) {
            throw new BusinessRuleViolationException(
                    "Insufficient stock for variant " + productVariantId
                            + ". Requested: " + quantity + ", available: " + inventory.sellable());
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory, InventoryMovement.MovementType.RESERVATION, -quantity, referenceType, referenceId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long productVariantId, int quantity, String referenceType, Long referenceId) {
        Inventory inventory = lockInventory(productVariantId);

        int newReserved = Math.max(0, inventory.getQuantityReserved() - quantity);
        inventory.setQuantityReserved(newReserved);
        inventoryRepository.save(inventory);

        recordMovement(inventory, InventoryMovement.MovementType.RESERVATION_RELEASE, quantity, referenceType, referenceId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void commitSale(Long productVariantId, int quantity, String referenceType, Long referenceId) {
        Inventory inventory = lockInventory(productVariantId);

        inventory.setQuantityAvailable(Math.max(0, inventory.getQuantityAvailable() - quantity));
        inventory.setQuantityReserved(Math.max(0, inventory.getQuantityReserved() - quantity));
        inventoryRepository.save(inventory);

        recordMovement(inventory, InventoryMovement.MovementType.SALE, -quantity, referenceType, referenceId);
    }

    @Transactional
    public void restock(Long productVariantId, int quantity, String reason, Long adminUserId) {
        Inventory inventory = lockInventory(productVariantId);
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);
        inventoryRepository.save(inventory);

        InventoryMovement movement = new InventoryMovement();
        movement.setInventory(inventory);
        movement.setMovementType(InventoryMovement.MovementType.RESTOCK);
        movement.setQuantityDelta(quantity);
        movement.setReason(reason);
        movement.setCreatedBy(adminUserId);
        inventoryMovementRepository.save(movement);
    }

    @Transactional
    public void returnStock(Long productVariantId, int quantity, String referenceType, Long referenceId) {
        Inventory inventory = lockInventory(productVariantId);
        inventory.setQuantityAvailable(inventory.getQuantityAvailable() + quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory, InventoryMovement.MovementType.RETURN, quantity, referenceType, referenceId);
    }

    private Inventory lockInventory(Long productVariantId) {
        return inventoryRepository.findByProductVariantIdForUpdate(productVariantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Inventory for variant", productVariantId));
    }

    private void recordMovement(Inventory inventory, InventoryMovement.MovementType type, int delta, String referenceType, Long referenceId) {
        InventoryMovement movement = new InventoryMovement();
        movement.setInventory(inventory);
        movement.setMovementType(type);
        movement.setQuantityDelta(delta);
        movement.setReferenceType(referenceType);
        movement.setReferenceId(referenceId);
        inventoryMovementRepository.save(movement);
    }
}
