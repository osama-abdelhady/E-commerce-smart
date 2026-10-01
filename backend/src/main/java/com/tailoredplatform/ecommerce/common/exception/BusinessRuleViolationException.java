package com.tailoredplatform.ecommerce.common.exception;

/** Thrown when a request is well-formed but violates a business invariant (e.g. insufficient stock, invalid order-status transition). */
public class BusinessRuleViolationException extends RuntimeException {
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
