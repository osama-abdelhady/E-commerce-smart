package com.tailoredplatform.ecommerce.reviews.service;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.orders.entity.Order;
import com.tailoredplatform.ecommerce.orders.entity.OrderItem;
import com.tailoredplatform.ecommerce.orders.repository.OrderItemRepository;
import com.tailoredplatform.ecommerce.products.entity.Product;
import com.tailoredplatform.ecommerce.products.repository.ProductRepository;
import com.tailoredplatform.ecommerce.reviews.dto.ReviewRequest;
import com.tailoredplatform.ecommerce.reviews.entity.Review;
import com.tailoredplatform.ecommerce.reviews.repository.ReviewRepository;
import com.tailoredplatform.ecommerce.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public Review create(User user, ReviewRequest request) {
        OrderItem orderItem = orderItemRepository.findById(request.orderItemId())
                .orElseThrow(() -> ResourceNotFoundException.of("OrderItem", request.orderItemId()));

        Order order = orderItem.getOrder();
        if (!order.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleViolationException("You can only review items from your own orders.");
        }
        // Only allow reviewing items from orders that actually completed —
        // PENDING (not yet paid) or CANCELLED orders can't be reviewed.
        if (order.getStatus() == Order.Status.PENDING || order.getStatus() == Order.Status.CANCELLED) {
            throw new BusinessRuleViolationException("You can only review items from a completed purchase.");
        }
        // Belt-and-suspenders: the DB has a unique constraint on
        // reviews.order_item_id too (V5 migration), so this can never be
        // bypassed even by a race between two concurrent requests — this
        // check just gives a friendlier error before hitting that constraint.
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new BusinessRuleViolationException("You have already reviewed this purchase.");
        }

        Product product = orderItem.getProductVariant().getProduct();

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setOrderItem(orderItem);
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setBody(request.body());
        review.setStatus(Review.Status.PUBLISHED);
        reviewRepository.save(review);

        recomputeProductRating(product);
        return review;
    }

    public Review update(User user, Long reviewId, ReviewRequest request) {
        Review review = requireOwned(user, reviewId);
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setBody(request.body());
        reviewRepository.save(review);

        recomputeProductRating(review.getProduct());
        return review;
    }

    public void delete(User user, Long reviewId) {
        Review review = requireOwned(user, reviewId);
        Product product = review.getProduct();
        reviewRepository.delete(review);
        recomputeProductRating(product);
    }

    @Transactional(readOnly = true)
    public Page<Review> listForProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatus(productId, Review.Status.PUBLISHED, pageable);
    }

    private Review requireOwned(User user, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ResourceNotFoundException.of("Review", reviewId));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleViolationException("You can only edit or delete your own reviews.");
        }
        return review;
    }

    private void recomputeProductRating(Product product) {
        // Simple approach: pull all PUBLISHED ratings for the product and
        // average them. Fine at this product-catalog scale; a
        // high-volume catalog would push this to a DB-side AVG() query
        // instead of loading every review into memory.
        List<Review> published = reviewRepository
                .findByProductIdAndStatus(product.getId(), Review.Status.PUBLISHED, org.springframework.data.domain.Pageable.unpaged())
                .getContent();

        if (published.isEmpty()) {
            product.setAverageRating(BigDecimal.ZERO);
            product.setReviewCount(0);
        } else {
            BigDecimal sum = published.stream()
                    .map(r -> BigDecimal.valueOf(r.getRating()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = sum.divide(BigDecimal.valueOf(published.size()), 2, RoundingMode.HALF_UP);
            product.setAverageRating(average);
            product.setReviewCount(published.size());
        }
        productRepository.save(product);
    }
}
