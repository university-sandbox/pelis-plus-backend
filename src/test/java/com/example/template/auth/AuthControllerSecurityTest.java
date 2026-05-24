package com.example.template.auth;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.template.domain.AppUser;
import com.example.template.domain.AppUserRepository;
import com.example.template.domain.Role;
import com.example.template.membership.ActiveMembership;
import com.example.template.membership.ActiveMembershipRepository;
import com.example.template.membership.MembershipPlan;
import com.example.template.membership.MembershipPlanRepository;
import com.example.template.security.JwtService;
import com.example.template.security.UserPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerSecurityTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ActiveMembershipRepository activeMembershipRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        activeMembershipRepository.deleteAll();
        membershipPlanRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meRejectsTokenForDeletedUser() throws Exception {
        AppUser user = createUser("deleted@example.com");
        String token = jwtService.generateToken(new UserPrincipal(user));
        appUserRepository.delete(user);
        appUserRepository.flush();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentUserForValidToken() throws Exception {
        AppUser user = createUser("user@example.com");
        String token = jwtService.generateToken(new UserPrincipal(user));

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(user.getId().toString()))
            .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void meReturnsCurrentUserWithActiveMembership() throws Exception {
        AppUser user = createUser("member@example.com");
        MembershipPlan plan = createPlan();
        ActiveMembership membership = new ActiveMembership();
        membership.setUser(user);
        membership.setPlan(plan);
        membership.setExpiresAt(LocalDate.now().plusMonths(1));
        activeMembershipRepository.saveAndFlush(membership);
        String token = jwtService.generateToken(new UserPrincipal(user));

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.membership.planId").value(plan.getId().toString()))
            .andExpect(jsonPath("$.membership.planName").value(plan.getName()));
    }

    private AppUser createUser(String email) {
        AppUser user = new AppUser();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        return appUserRepository.saveAndFlush(user);
    }

    private MembershipPlan createPlan() {
        MembershipPlan plan = new MembershipPlan();
        plan.setName("Gold");
        plan.setPrice(BigDecimal.TEN);
        plan.setValidity("monthly");
        plan.setDiscountPercentage(20);
        plan.setTicketsPerMonth(4);
        return membershipPlanRepository.saveAndFlush(plan);
    }
}
