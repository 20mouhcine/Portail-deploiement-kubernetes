package org.example.auth.service;

import org.example.auth.entity.Role;
import org.example.auth.enums.RoleName;

import java.util.Set;

public interface RoleService {

    Role getByName(RoleName name);

    Set<Role> getByNames(Set<RoleName> names);
}
