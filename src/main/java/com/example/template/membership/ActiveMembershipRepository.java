package com.example.template.membership;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveMembershipRepository extends JpaRepository<ActiveMembership, UUID> {

    Optional<ActiveMembership> findByUserId(UUID userId);
}
