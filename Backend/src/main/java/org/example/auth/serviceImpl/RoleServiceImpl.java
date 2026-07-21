package org.example.auth.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.example.auth.entity.Role;
import org.example.auth.enums.RoleName;
import org.example.auth.exception.ResourceNotFoundException;
import org.example.auth.repository.RoleRepository;
import org.example.auth.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Role getByName(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Le rôle " + name + " n'existe pas"
                ));
    }

    @Override
    public Set<Role> getByNames(Set<RoleName> names) {
        return names.stream()
                .map(this::getByName)
                .collect(Collectors.toSet());
    }
}
