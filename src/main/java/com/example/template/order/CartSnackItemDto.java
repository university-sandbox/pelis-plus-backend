package com.example.template.order;

import java.util.Map;

public record CartSnackItemDto(
    String id,
    String snackId,
    String snackName,
    Integer quantity,
    Double unitPrice,
    Map<String, String> selectedOptions
) {
}
