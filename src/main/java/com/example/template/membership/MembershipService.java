// TODO: registrar log de auditoría cuando se aplique descuento de membresía
package com.example.template.membership;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import com.example.template.payment.StripeCheckoutSession;
import com.example.template.payment.StripePaymentService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    private final StripePaymentService stripePaymentService;

    public MembershipService(
        MembershipPlanRepository planRepository,
        MembershipPlanBenefitRepository benefitRepository,
        ActiveMembershipRepository activeMembershipRepository,
        AppUserRepository appUserRepository,
        StripePaymentService stripePaymentService
    ) {
        this.planRepository = planRepository;
        this.benefitRepository = benefitRepository;
        this.activeMembershipRepository = activeMembershipRepository;
        this.appUserRepository = appUserRepository;
        this.stripePaymentService = stripePaymentService;
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
    public MembershipSubscriptionResponse subscribe(UUID planId, UUID userId) {
        MembershipPlan plan = planRepository.findById(planId)
            .orElseThrow(() -> new EntityNotFoundException("Plan not found: " + planId));

        StripeCheckoutSession checkoutSession = stripePaymentService.createMembershipCheckoutSession(plan, userId);
        return new MembershipSubscriptionResponse(planId.toString(), checkoutSession.id(), checkoutSession.url());
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
    public ActiveMembershipDto confirmStripeCheckout(String sessionId, UUID userId) {
        StripeCheckoutSession checkoutSession = stripePaymentService.retrieveCheckoutSession(sessionId);
        if (!stripePaymentService.isPaidOrNoPaymentRequired(checkoutSession)) {
            throw new IllegalArgumentException("Stripe checkout session is not paid");
        }

        Map<String, String> metadata = stripePaymentService.retrieveCheckoutSessionMetadata(sessionId);
        if (metadata == null || !userId.toString().equals(metadata.get("user_id"))) {
            throw new IllegalArgumentException("Stripe checkout session does not belong to this user");
        }

        String planId = metadata.get("plan_id");
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("Stripe checkout session is missing membership plan metadata");
        }

        return confirmSubscription(UUID.fromString(planId), userId);
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
