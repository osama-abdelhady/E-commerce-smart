package com.tailoredplatform.ecommerce.inventory.controller;

import com.tailoredplatform.ecommerce.inventory.dto.InventoryResponse;
import com.tailoredplatform.ecommerce.inventory.dto.RestockRequest;
import com.tailoredplatform.ecommerce.inventory.entity.Inventory;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import com.tailoredplatform.ecommerce.inventory.service.InventoryService;
import com.tailoredplatform.ecommerce.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Inventory", description = "Stock levels, restocking, low-stock alerts")
public class AdminInventoryController {

    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "List all inventory rows")
    public List<InventoryResponse> list() {
        return inventoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List variants at or below their low-stock threshold")
    public List<InventoryResponse> lowStock() {
        // Uses the per-row threshold rather than one global number — filtered
        // in memory since each row's threshold can differ. Fine at this
        // catalog's scale; the Phase 9 performance note applies here too if
        // the catalog grows much larger.
        return inventoryRepository.findAll().stream()
                .filter(inv -> inv.getQuantityAvailable() - inv.getQuantityReserved() <= inv.getLowStockThreshold())
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{variantId}/restock")
    @Operation(summary = "Add stock for a variant (writes an InventoryMovement audit row)")
    public void restock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long variantId,
            @Valid @RequestBody RestockRequest request
    ) {
        inventoryService.restock(variantId, request.quantity(), request.reason(), principal.getId());
    }

    private InventoryResponse toResponse(Inventory inv) {
        var variant = inv.getProductVariant();
        return new InventoryResponse(
                inv.getId(), variant.getId(), variant.getSku(), variant.getProduct().getName(),
                inv.getQuantityAvailable(), inv.getQuantityReserved(), inv.sellable(),
                inv.getLowStockThreshold(), inv.isLowStock()
        );
    }
}
