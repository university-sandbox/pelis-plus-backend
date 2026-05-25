package com.example.template.membership;

import com.example.template.security.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/memberships")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<MembershipPlanDto>> getPlans() {
        return ResponseEntity.ok(membershipService.getPlans());
    }

    @GetMapping("/me")
    public ResponseEntity<ActiveMembershipDto> getMyPlan(@AuthenticationPrincipal UserPrincipal principal) {
        ActiveMembershipDto dto = membershipService.getMyPlan(principal.getUser().getId());
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/me/benefits")
    public ResponseEntity<List<BenefitDto>> getMyBenefits(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(membershipService.getMyBenefits(principal.getUser().getId()));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<MembershipSubscriptionResponse> subscribe(
        @RequestBody SubscribeRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(membershipService.subscribe(request.planId(), principal.getUser().getId()));
    }

    @PostMapping("/me/confirm")
    public ResponseEntity<ActiveMembershipDto> confirmSubscription(
        @RequestBody SubscribeRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(membershipService.confirmSubscription(request.planId(), principal.getUser().getId()));
    }

    @PostMapping("/stripe/confirm")
    public ResponseEntity<ActiveMembershipDto> confirmStripeCheckout(
        @Valid @RequestBody ConfirmMembershipStripePayload payload,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(membershipService.confirmStripeCheckout(payload.sessionId(), principal.getUser().getId()));
    }

    @PatchMapping("/me/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal UserPrincipal principal) {
        membershipService.cancel(principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }
}
