package com.tailoredplatform.ecommerce.admin.dto;

import java.util.List;
import java.util.Map;

public record OrderStatusReportResponse(
        Map<String, Long> countsByStatus
) {
}
