package com.tailoredplatform.ecommerce.reviews.repository;

import com.tailoredplatform.ecommerce.reviews.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdAndStatus(Long productId, Review.Status status, Pageable pageable);
    boolean existsByOrderItemId(Long orderItemId);
    Optional<Review> findByOrderItemId(Long orderItemId);
}
