package com.example.template.seat;

import java.util.List;
import java.util.UUID;

public record ReserveSeatsRequest(List<UUID> seatIds) {
}
