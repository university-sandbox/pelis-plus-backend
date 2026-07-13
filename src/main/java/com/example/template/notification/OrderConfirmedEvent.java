package com.example.template.notification;

import java.util.UUID;

public record OrderConfirmedEvent(UUID orderId) {
}
