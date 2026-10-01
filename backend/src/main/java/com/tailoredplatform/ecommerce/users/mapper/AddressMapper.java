package com.tailoredplatform.ecommerce.users.mapper;

import com.tailoredplatform.ecommerce.users.dto.AddressResponse;
import com.tailoredplatform.ecommerce.users.entity.Address;
import org.springframework.stereotype.Component;

/**
 * A plain @Component rather than @Mapper: Address's boolean field is named
 * isDefault, whose Lombok-generated getter (isDefault()) is JavaBean-
 * introspected as property "default" — but AddressResponse's record
 * component is named isDefault. Letting MapStruct auto-match those two
 * different property names risked a silently unmapped field; explicit
 * mapping removes the ambiguity entirely.
 */
@Component
public class AddressMapper {
    public AddressResponse toResponse(Address address) {
        if (address == null) return null;
        return new AddressResponse(
                address.getId(), address.getLabel(), address.getFullName(),
                address.getLine1(), address.getLine2(), address.getCity(), address.getState(),
                address.getPostalCode(), address.getCountry(), address.getPhone(), address.isDefault()
        );
    }
}
