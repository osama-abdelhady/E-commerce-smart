package com.tailoredplatform.ecommerce.reviews.mapper;

import com.tailoredplatform.ecommerce.reviews.dto.ReviewResponse;
import com.tailoredplatform.ecommerce.reviews.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {
    public ReviewResponse toResponse(Review review) {
        // First name + last initial, not the full name, to give reviewers
        // some privacy on a public product page.
        String[] parts = review.getUser().getFullName().trim().split("\\s+", 2);
        String displayName = parts.length > 1
                ? parts[0] + " " + parts[1].charAt(0) + "."
                : parts[0];

        return new ReviewResponse(
                review.getId(), review.getProduct().getId(), displayName,
                review.getRating(), review.getTitle(), review.getBody(), review.getCreatedAt()
        );
    }
}
