package com.tailoredplatform.ecommerce.users.service;

import com.tailoredplatform.ecommerce.common.exception.BusinessRuleViolationException;
import com.tailoredplatform.ecommerce.common.exception.ResourceNotFoundException;
import com.tailoredplatform.ecommerce.users.dto.AddressRequest;
import com.tailoredplatform.ecommerce.users.entity.Address;
import com.tailoredplatform.ecommerce.users.entity.User;
import com.tailoredplatform.ecommerce.users.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressService {

    private final AddressRepository addressRepository;

    public List<Address> list(User user) {
        return addressRepository.findByUserId(user.getId());
    }

    public Address create(User user, AddressRequest request) {
        Address address = new Address();
        address.setUser(user);
        applyFields(address, request);

        boolean isFirstAddress = addressRepository.findByUserId(user.getId()).isEmpty();
        if (request.isDefault() || isFirstAddress) {
            clearExistingDefault(user);
            address.setDefault(true);
        }

        return addressRepository.save(address);
    }

    public Address update(User user, Long addressId, AddressRequest request) {
        Address address = requireOwned(user, addressId);
        applyFields(address, request);

        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(user);
            address.setDefault(true);
        }

        return addressRepository.save(address);
    }

    public void delete(User user, Long addressId) {
        Address address = requireOwned(user, addressId);
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        // Promote another address to default so checkout never has zero
        // default addresses while the user still has at least one saved.
        if (wasDefault) {
            addressRepository.findByUserId(user.getId()).stream().findFirst().ifPresent(next -> {
                next.setDefault(true);
                addressRepository.save(next);
            });
        }
    }

    private void clearExistingDefault(User user) {
        addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .forEach(existing -> {
                    existing.setDefault(false);
                    addressRepository.save(existing);
                });
    }

    private void applyFields(Address address, AddressRequest request) {
        address.setLabel(request.label());
        address.setFullName(request.fullName());
        address.setLine1(request.line1());
        address.setLine2(request.line2());
        address.setCity(request.city());
        address.setState(request.state());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country().toUpperCase());
        address.setPhone(request.phone());
    }

    private Address requireOwned(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> ResourceNotFoundException.of("Address", addressId));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new BusinessRuleViolationException("Address does not belong to the current user.");
        }
        return address;
    }
}
