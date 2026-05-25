package com.example.template.payment;

import com.example.template.order.Order;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentService {

    private final StripePaymentProperties properties;

    public StripePaymentService(StripePaymentProperties properties) {
        this.properties = properties;
    }

    public StripeCheckoutSession createCheckoutSession(Order order) {
        ensureConfigured();

        try {
            Stripe.apiKey = properties.secretKey();
            SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(order.getId().toString())
                .setSuccessUrl(resolveUrl(properties.successUrl(), order))
                .setCancelUrl(resolveUrl(properties.cancelUrl(), order))
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(properties.currency())
                                .setUnitAmount(toMinorUnits(order.getTotal()))
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Pelis Plus - Pedido " + order.getId())
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .putMetadata("order_id", order.getId().toString())
                .putMetadata("user_id", order.getUser().getId().toString())
                .build();

            Session session = Session.create(params);
            return new StripeCheckoutSession(session.getId(), session.getUrl(), session.getPaymentStatus());
        } catch (StripeException ex) {
            throw new IllegalStateException("No pudimos crear la sesión de pago en Stripe", ex);
        }
    }

    public StripeCheckoutSession retrieveCheckoutSession(String sessionId) {
        ensureConfigured();

        try {
            Stripe.apiKey = properties.secretKey();
            Session session = Session.retrieve(sessionId);
            return new StripeCheckoutSession(session.getId(), session.getUrl(), session.getPaymentStatus());
        } catch (StripeException ex) {
            throw new IllegalStateException("No pudimos verificar la sesión de pago en Stripe", ex);
        }
    }

    private void ensureConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY is required to use Stripe checkout");
        }
    }

    private String resolveUrl(String template, Order order) {
        return template
            .replace("{ORDER_ID}", order.getId().toString())
            .replace("{CHECKOUT_SESSION_ID}", "{CHECKOUT_SESSION_ID}");
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount
            .multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    }
}
