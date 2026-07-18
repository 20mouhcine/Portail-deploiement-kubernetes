package org.example.backend.serviceimpl;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.Role;
import org.example.backend.enums.RoleName;
import org.example.backend.exception.ResourceNotFoundException;
import org.example.backend.repository.RoleRepository;
import org.example.backend.service.RoleService;
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
