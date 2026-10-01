package com.tailoredplatform.ecommerce.inventory;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.inventory.entity.Inventory;
import com.tailoredplatform.ecommerce.inventory.entity.InventoryMovement;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryMovementRepository;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import com.tailoredplatform.ecommerce.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryMovementRepository inventoryMovementRepository;

    private InventoryService inventoryService;

    private static final Long VARIANT_ID = 42L;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, inventoryMovementRepository);
    }

    private Inventory inventoryWith(int available, int reserved) {
        Inventory inv = new Inventory();
        inv.setQuantityAvailable(available);
        inv.setQuantityReserved(reserved);
        return inv;
    }

    @Test
    void reserve_succeeds_whenEnoughSellableStock() {
        Inventory inv = inventoryWith(10, 2); // sellable = 8
        when(inventoryRepository.findByProductVariantIdForUpdate(VARIANT_ID)).thenReturn(Optional.of(inv));

        inventoryService.reserve(VARIANT_ID, 5, "ORDER", 100L);

        assertThat(inv.getQuantityReserved()).isEqualTo(7); // 2 + 5
        assertThat(inv.getQuantityAvailable()).isEqualTo(10); // unchanged — reservation, not a sale

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(InventoryMovement.MovementType.RESERVATION);
        assertThat(movementCaptor.getValue().getQuantityDelta()).isEqualTo(-5);
    }

    @Test
    void reserve_throws_whenRequestedExceedsSellable() {
        Inventory inv = inventoryWith(5, 4); // sellable = 1
        when(inventoryRepository.findByProductVariantIdForUpdate(VARIANT_ID)).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> inventoryService.reserve(VARIANT_ID, 2, "ORDER", 100L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Insufficient stock");

        // Nothing should have been mutated or persisted on the failed path.
        assertThat(inv.getQuantityReserved()).isEqualTo(4);
        verify(inventoryRepository, never()).save(any());
        verify(inventoryMovementRepository, never()).save(any());
    }

    @Test
    void commitSale_decreasesBothAvailableAndReserved() {
        Inventory inv = inventoryWith(10, 5);
        when(inventoryRepository.findByProductVariantIdForUpdate(VARIANT_ID)).thenReturn(Optional.of(inv));

        inventoryService.commitSale(VARIANT_ID, 3, "ORDER", 100L);

        assertThat(inv.getQuantityAvailable()).isEqualTo(7);
        assertThat(inv.getQuantityReserved()).isEqualTo(2);

        ArgumentCaptor<InventoryMovement> movementCaptor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(inventoryMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getMovementType()).isEqualTo(InventoryMovement.MovementType.SALE);
    }

    @Test
    void release_returnsReservedStockWithoutTouchingAvailable() {
        Inventory inv = inventoryWith(10, 5);
        when(inventoryRepository.findByProductVariantIdForUpdate(VARIANT_ID)).thenReturn(Optional.of(inv));

        inventoryService.release(VARIANT_ID, 5, "ORDER_CANCELLED", 100L);

        assertThat(inv.getQuantityReserved()).isEqualTo(0);
        assertThat(inv.getQuantityAvailable()).isEqualTo(10); // never touched by release

        verify(inventoryMovementRepository).save(argThatMovementType(InventoryMovement.MovementType.RESERVATION_RELEASE));
    }

    @Test
    void release_neverGoesNegative_evenIfCalledWithMoreThanIsReserved() {
        Inventory inv = inventoryWith(10, 2);
        when(inventoryRepository.findByProductVariantIdForUpdate(VARIANT_ID)).thenReturn(Optional.of(inv));

        inventoryService.release(VARIANT_ID, 5, "ORDER_CANCELLED", 100L); // releasing more than reserved

        assertThat(inv.getQuantityReserved()).isEqualTo(0); // clamped, not -3
    }

    @Test
    void returnStock_addsBackToAvailable_forAReversedSale() {
        Inventory inv = inventoryWith(10, 0);
        when(inventoryRepository.findByProductVariantIdForUpdate(VARIANT_ID)).thenReturn(Optional.of(inv));

        inventoryService.returnStock(VARIANT_ID, 4, "ORDER_CANCELLED", 100L);

        assertThat(inv.getQuantityAvailable()).isEqualTo(14);
        verify(inventoryMovementRepository, times(1)).save(argThatMovementType(InventoryMovement.MovementType.RETURN));
    }

    private InventoryMovement argThatMovementType(InventoryMovement.MovementType type) {
        return org.mockito.ArgumentMatchers.argThat(m -> m.getMovementType() == type);
    }
}
