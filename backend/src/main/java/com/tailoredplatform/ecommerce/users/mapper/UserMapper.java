package com.tailoredplatform.ecommerce.users.mapper;

import com.tailoredplatform.ecommerce.users.dto.UserResponse;
import com.tailoredplatform.ecommerce.users.entity.Role;
import com.tailoredplatform.ecommerce.users.entity.User;
import org.mapstruct.Mapper;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    default UserResponse toResponse(User user) {
        if (user == null) return null;
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new UserResponse(
                user.getId(), user.getEmail(), user.getFullName(),
                user.getPhone(), user.getStatus().name(), roleNames
        );
    }
}
