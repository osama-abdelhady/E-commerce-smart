package com.tailoredplatform.ecommerce.inventory;

import com.tailoredplatform.ecommerce.categories.entity.Category;
import com.tailoredplatform.ecommerce.categories.repository.CategoryRepository;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.inventory.entity.Inventory;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import com.tailoredplatform.ecommerce.inventory.service.InventoryService;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import com.tailoredplatform.ecommerce.products.repository.ProductRepository;
import com.tailoredplatform.ecommerce.products.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the test the spec explicitly calls for: "Concurrent inventory
 * tests" and "Do not assume that a single @Transactional annotation
 * automatically solves every concurrency ... problem." It proves the claim
 * made throughout this build's README — that InventoryRepository's
 * PESSIMISTIC_WRITE lock (Phase 2) actually prevents two simultaneous
 * checkouts from both claiming the last unit of stock — with two REAL
 * threads and REAL separate database transactions, not a mock.
 *
 * reserve()/release()/commitSale() require Propagation.MANDATORY (they must
 * run inside a caller's transaction), so each thread here wraps its call in
 * its own TransactionTemplate-managed transaction, exactly as OrderService
 * would in production.
 */
@SpringBootTest
@ActiveProfiles("test")
class InventoryConcurrencyIT {

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository productVariantRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private InventoryService inventoryService;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void onlyOneOfTwoConcurrentReservations_succeeds_whenExactlyOneUnitIsInStock() throws InterruptedException {
        Long variantId = seedSingleUnitOfStock();

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        Runnable attemptReservation = () -> {
            try {
                startGate.await(); // both threads fire as close to simultaneously as possible
                txTemplate.executeWithoutResult(status ->
                        inventoryService.reserve(variantId, 1, "CONCURRENCY_TEST", null));
                successCount.incrementAndGet();
            } catch (BusinessRuleViolationException e) {
                insufficientStockCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(attemptReservation);
        executor.submit(attemptReservation);
        startGate.countDown(); // release both threads at once
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).as("both threads should finish within the timeout").isTrue();
        assertThat(successCount.get())
                .as("exactly one of the two concurrent reservations for the last unit should succeed")
                .isEqualTo(1);
        assertThat(insufficientStockCount.get())
                .as("the other should fail with insufficient stock, not silently oversell")
                .isEqualTo(1);

        Inventory finalState = inventoryRepository.findByProductVariantId(variantId).orElseThrow();
        assertThat(finalState.getQuantityAvailable()).isEqualTo(1); // still 1 — nothing was sold, only reserved
        assertThat(finalState.getQuantityReserved()).isEqualTo(1); // exactly one reservation held
        assertThat(finalState.sellable()).isEqualTo(0);
    }

    private Long seedSingleUnitOfStock() {
        Category category = new Category();
        category.setSlug("concurrency-test-category-" + System.nanoTime());
        category.setName("Concurrency Test Category");
        category.setActive(true);
        category.setDisplayOrder(0);
        categoryRepository.save(category);

        Product product = new Product();
        product.setSku("CONC-TEST-" + System.nanoTime());
        product.setSlug("concurrency-test-" + System.nanoTime());
        product.setName("Concurrency Test Product");
        product.setPrice(new BigDecimal("99.00"));
        product.setCategory(category);
        product.setStatus(Product.Status.ACTIVE);
        productRepository.save(product);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSku("CONC-TEST-VAR-" + System.nanoTime());
        variant.setActive(true);
        productVariantRepository.save(variant);

        Inventory inventory = new Inventory();
        inventory.setProductVariant(variant);
        inventory.setQuantityAvailable(1); // exactly one unit — the whole point of the test
        inventory.setQuantityReserved(0);
        inventoryRepository.save(inventory);

        return variant.getId();
    }
}
