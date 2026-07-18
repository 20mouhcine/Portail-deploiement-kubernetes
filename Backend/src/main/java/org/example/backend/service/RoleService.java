package org.example.backend.service;

import org.example.backend.entity.Role;
import org.example.backend.enums.RoleName;

import java.util.Set;

public interface RoleService {

    Role getByName(RoleName name);

    Set<Role> getByNames(Set<RoleName> names);
}
