package com.tailoredplatform.ecommerce.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the REAL SecurityConfig filter chain end-to-end (not mocked),
 * proving the spec's explicit requirement — "Hiding admin pages in Angular
 * is not sufficient security" — actually holds at the backend boundary.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthorizationIT {

    @Autowired private MockMvc mockMvc;

    @Test
    void publicCatalogEndpoint_isReachableWithNoAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    void adminDashboard_rejectsAnUnauthenticatedRequest_with401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminDashboard_rejectsAnAuthenticatedNonAdmin_with403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDashboard_isReachable_forAnAuthenticatedAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    void cartEndpoint_requiresAuthentication_evenThoughItIsNotUnderAdmin() throws Exception {
        // Guards against a common regression: an endpoint added later without
        // realizing SecurityConfig's default is authenticated(), not permitAll().
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshEndpoint_rejectsAMissingCsrfToken_becauseItIsCookieAuthenticated() throws Exception {
        // /auth/refresh is one of the two endpoints SecurityConfig scopes CSRF
        // protection to (see SecurityConfig's javadoc) — posting without a
        // CSRF token must be rejected even though the endpoint is otherwise public.
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshEndpoint_acceptsTheRequest_whenAValidCsrfTokenIsPresent() throws Exception {
        // With a valid CSRF token but no refresh-token cookie, this should get
        // past CSRF and fail for a business reason (no cookie) — never a 403
        // from Spring Security's CSRF filter itself.
        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()))
                .andExpect(status().is4xxClientError())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
    }
}
