package com.example.template.membership;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipPlanBenefitRepository extends JpaRepository<MembershipPlanBenefit, UUID> {

    List<MembershipPlanBenefit> findByPlanIdOrderByDisplayOrder(UUID planId);
}
