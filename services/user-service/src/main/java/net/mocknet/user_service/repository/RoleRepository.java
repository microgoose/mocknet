package net.mocknet.user_service.repository;

import net.mocknet.user_service.model.user.Role;
import net.mocknet.user_service.model.user.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByName(RoleName name);
}
