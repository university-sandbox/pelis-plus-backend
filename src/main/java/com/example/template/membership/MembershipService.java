package com.example.template.membership;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MembershipService {

    private final MembershipPlanRepository planRepository;
    private final MembershipPlanBenefitRepository benefitRepository;
    private final ActiveMembershipRepository activeMembershipRepository;
    private final AppUserRepository appUserRepository;

    public MembershipService(
        MembershipPlanRepository planRepository,
        MembershipPlanBenefitRepository benefitRepository,
        ActiveMembershipRepository activeMembershipRepository,
        AppUserRepository appUserRepository
    ) {
        this.planRepository = planRepository;
        this.benefitRepository = benefitRepository;
        this.activeMembershipRepository = activeMembershipRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<MembershipPlanDto> getPlans() {
        return planRepository.findAllByOrderByPrice().stream()
            .map(this::toPlanDto)
            .toList();
    }

    public ActiveMembershipDto getMyPlan(UUID userId) {
        return activeMembershipRepository.findByUserId(userId)
            .map(this::toActiveMembershipDto)
            .orElse(null);
    }

    public List<BenefitDto> getMyBenefits(UUID userId) {
        Optional<ActiveMembership> membership = activeMembershipRepository.findByUserId(userId);
        if (membership.isEmpty()) {
            return List.of();
        }
        return benefitRepository.findByPlanIdOrderByDisplayOrder(membership.get().getPlan().getId())
            .stream()
            .map(b -> new BenefitDto(b.getLabel(), b.getDescription()))
            .toList();
    }

    @Transactional
    public String subscribe(UUID planId, UUID userId) {
        // Return a mock form token for payment
        String formToken = "MOCK_IZIPAY_MEMBERSHIP_" + userId.toString().substring(0, 8).toUpperCase() + "_" + planId.toString().substring(0, 8).toUpperCase();
        return formToken;
    }

    @Transactional
    public ActiveMembershipDto confirmSubscription(UUID planId, UUID userId) {
        AppUser user = appUserRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        MembershipPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new EntityNotFoundException("Plan not found: " + planId));

        ActiveMembership membership = activeMembershipRepository.findByUserId(userId)
            .orElse(new ActiveMembership());

        membership.setUser(user);
        membership.setPlan(plan);
        membership.setExpiresAt(LocalDate.now().plusMonths(1));
        membership.setTicketsUsed(0);
        membership.setDiscountUsed(java.math.BigDecimal.ZERO);

        return toActiveMembershipDto(activeMembershipRepository.save(membership));
    }

    @Transactional
    public void cancel(UUID userId) {
        activeMembershipRepository.findByUserId(userId)
            .ifPresent(activeMembershipRepository::delete);
    }

    private MembershipPlanDto toPlanDto(MembershipPlan plan) {
        List<BenefitDto> benefits = benefitRepository.findByPlanIdOrderByDisplayOrder(plan.getId())
            .stream()
            .map(b -> new BenefitDto(b.getLabel(), b.getDescription()))
            .toList();

        return new MembershipPlanDto(
            plan.getId().toString(),
            plan.getName(),
            plan.getPrice() != null ? plan.getPrice().doubleValue() : 0.0,
            plan.getValidity(),
            benefits,
            plan.getDiscountPercentage(),
            plan.getTicketsPerMonth(),
            plan.getRecommended(),
            plan.getColor()
        );
    }

    private ActiveMembershipDto toActiveMembershipDto(ActiveMembership m) {
        return new ActiveMembershipDto(
            m.getPlan().getId().toString(),
            m.getPlan().getName(),
            m.getExpiresAt().toString(),
            m.getTicketsUsed(),
            m.getPlan().getTicketsPerMonth(),
            m.getDiscountUsed() != null ? m.getDiscountUsed().doubleValue() : 0.0
        );
    }
}
