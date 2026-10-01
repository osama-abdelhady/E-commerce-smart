package com.tailoredplatform.ecommerce.products.service;

import com.tailoredplatform.ecommerce.categories.entity.Brand;
import com.tailoredplatform.ecommerce.categories.entity.Category;
import com.tailoredplatform.ecommerce.categories.repository.BrandRepository;
import com.tailoredplatform.ecommerce.categories.repository.CategoryRepository;
import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.inventory.entity.Inventory;
import com.tailoredplatform.ecommerce.inventory.repository.InventoryRepository;
import com.tailoredplatform.ecommerce.products.dto.ProductCreateRequest;
import com.tailoredplatform.ecommerce.products.dto.ProductUpdateRequest;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.entity.ProductImage;
import com.tailoredplatform.ecommerce.products.entity.ProductVariant;
import com.tailoredplatform.ecommerce.products.repository.ProductRepository;
import com.tailoredplatform.ecommerce.products.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final InventoryRepository inventoryRepository;

    public Product create(ProductCreateRequest request) {
        if (productRepository.findBySku(request.sku()).isPresent()) {
            throw new BusinessRuleViolationException("A product with SKU " + request.sku() + " already exists.");
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", request.categoryId()));
        Brand brand = request.brandId() != null
                ? brandRepository.findById(request.brandId()).orElseThrow(() -> ResourceNotFoundException.of("Brand", request.brandId()))
                : null;

        Product product = new Product();
        product.setSku(request.sku());
        product.setSlug(request.slug());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setDiscountPrice(request.discountPrice());
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus(Product.Status.DRAFT);
        product.setBestSeller(request.isBestSeller());
        product.setNewArrival(request.isNewArrival());
        productRepository.save(product);

        if (request.images() != null) {
            for (var imgInput : request.images()) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setUrl(imgInput.url());
                image.setAltText(imgInput.altText());
                image.setDisplayOrder(imgInput.displayOrder());
                product.getImages().add(image);
            }
        }

        if (request.variants() != null) {
            for (var variantInput : request.variants()) {
                if (productVariantRepository.findBySku(variantInput.sku()).isPresent()) {
                    throw new BusinessRuleViolationException("A variant with SKU " + variantInput.sku() + " already exists.");
                }
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                variant.setSku(variantInput.sku());
                variant.setSize(variantInput.size());
                variant.setColor(variantInput.color());
                variant.setColorHex(variantInput.colorHex());
                variant.setPriceOverride(variantInput.priceOverride());
                product.getVariants().add(variant);
                productVariantRepository.save(variant);

                Inventory inventory = new Inventory();
                inventory.setProductVariant(variant);
                inventory.setQuantityAvailable(variantInput.initialQuantity());
                inventoryRepository.save(inventory);
            }
        }

        return productRepository.save(product);
    }

    public Product update(Long productId, ProductUpdateRequest request) {
        Product product = requireProduct(productId);

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> ResourceNotFoundException.of("Category", request.categoryId()));
        Brand brand = request.brandId() != null
                ? brandRepository.findById(request.brandId()).orElseThrow(() -> ResourceNotFoundException.of("Brand", request.brandId()))
                : null;

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setDiscountPrice(request.discountPrice());
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus(Product.Status.valueOf(request.status().toUpperCase()));
        product.setBestSeller(request.isBestSeller());
        product.setNewArrival(request.isNewArrival());

        return productRepository.save(product);
    }

    /** Soft-delete: deactivate rather than hard-delete, preserving order history integrity (order_items references product_variants). */
    public void deactivate(Long productId) {
        Product product = requireProduct(productId);
        product.setStatus(Product.Status.INACTIVE);
        productRepository.save(product);
    }

    public ProductImage addImage(Long productId, String url, String altText, int displayOrder) {
        Product product = requireProduct(productId);
        ProductImage image = new ProductImage();
        image.setProduct(product);
        image.setUrl(url);
        image.setAltText(altText);
        image.setDisplayOrder(displayOrder);
        product.getImages().add(image);
        productRepository.save(product);
        return image;
    }

    public void removeImage(Long productId, Long imageId) {
        Product product = requireProduct(productId);
        product.getImages().removeIf(img -> img.getId().equals(imageId));
        productRepository.save(product);
    }

    private Product requireProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
    }
}
