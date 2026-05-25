package com.example.template.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.template.domain.AppUserRepository;
import com.example.template.payment.StripeCheckoutSession;
import com.example.template.payment.StripePaymentService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipServiceTest {

    private final MembershipPlanRepository planRepository = mock(MembershipPlanRepository.class);
    private final MembershipPlanBenefitRepository benefitRepository = mock(MembershipPlanBenefitRepository.class);
    private final ActiveMembershipRepository activeMembershipRepository = mock(ActiveMembershipRepository.class);
    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final StripePaymentService stripePaymentService = mock(StripePaymentService.class);
    private final MembershipService service = new MembershipService(
        planRepository,
        benefitRepository,
        activeMembershipRepository,
        appUserRepository,
        stripePaymentService
    );

    @Test
    void subscribeReturnsStripeCheckoutSession() {
        UUID planId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        MembershipPlan plan = plan(planId);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(stripePaymentService.createMembershipCheckoutSession(plan, userId))
            .thenReturn(new StripeCheckoutSession("cs_test_membership", "https://checkout.stripe.test/session", "unpaid"));

        MembershipSubscriptionResponse response = service.subscribe(planId, userId);

        assertThat(response.planId()).isEqualTo(planId.toString());
        assertThat(response.checkoutSessionId()).isEqualTo("cs_test_membership");
        assertThat(response.checkoutUrl()).isEqualTo("https://checkout.stripe.test/session");
    }

    @Test
    void confirmStripeCheckoutRejectsSessionForAnotherUser() {
        UUID userId = UUID.randomUUID();
        when(stripePaymentService.retrieveCheckoutSession("cs_test_membership"))
            .thenReturn(new StripeCheckoutSession("cs_test_membership", null, "paid"));
        when(stripePaymentService.isPaidOrNoPaymentRequired(new StripeCheckoutSession("cs_test_membership", null, "paid")))
            .thenReturn(true);
        when(stripePaymentService.retrieveCheckoutSessionMetadata("cs_test_membership"))
            .thenReturn(Map.of("user_id", UUID.randomUUID().toString(), "plan_id", UUID.randomUUID().toString()));

        assertThatThrownBy(() -> service.confirmStripeCheckout("cs_test_membership", userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not belong");
    }

    private MembershipPlan plan(UUID planId) {
        MembershipPlan plan = new MembershipPlan();
        plan.setId(planId);
        plan.setName("Oro");
        plan.setPrice(BigDecimal.valueOf(59));
        plan.setValidity("1 mes");
        plan.setTicketsPerMonth(4);
        plan.setDiscountPercentage(20);
        return plan;
    }
}
