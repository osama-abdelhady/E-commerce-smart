package com.tailoredplatform.ecommerce.users.controller;

import com.tailoredplatform.ecommerce.security.UserPrincipal;
import com.tailoredplatform.ecommerce.users.dto.AddressRequest;
import com.tailoredplatform.ecommerce.users.dto.AddressResponse;
import com.tailoredplatform.ecommerce.users.dto.UpdateProfileRequest;
import com.tailoredplatform.ecommerce.users.dto.UserResponse;
import com.tailoredplatform.ecommerce.users.mapper.AddressMapper;
import com.tailoredplatform.ecommerce.users.mapper.UserMapper;
import com.tailoredplatform.ecommerce.users.service.AddressService;
import com.tailoredplatform.ecommerce.users.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Current user's profile and address book")
public class UserController {

    private final UserProfileService userProfileService;
    private final AddressService addressService;
    private final UserMapper userMapper;
    private final AddressMapper addressMapper;

    @GetMapping
    @Operation(summary = "Get the current user's profile")
    public UserResponse getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return userMapper.toResponse(principal.getUser());
    }

    @PutMapping
    @Operation(summary = "Update the current user's profile (name, phone)")
    public UserResponse updateProfile(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return userMapper.toResponse(userProfileService.updateProfile(principal.getId(), request));
    }

    @GetMapping("/addresses")
    @Operation(summary = "List the current user's saved addresses")
    public List<AddressResponse> listAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        return addressService.list(principal.getUser()).stream().map(addressMapper::toResponse).toList();
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a new address")
    public AddressResponse addAddress(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody AddressRequest request) {
        return addressMapper.toResponse(addressService.create(principal.getUser(), request));
    }

    @PutMapping("/addresses/{id}")
    @Operation(summary = "Update an existing address")
    public AddressResponse updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request
    ) {
        return addressMapper.toResponse(addressService.update(principal.getUser(), id, request));
    }

    @DeleteMapping("/addresses/{id}")
    @Operation(summary = "Delete an address")
    public void deleteAddress(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        addressService.delete(principal.getUser(), id);
    }
}
