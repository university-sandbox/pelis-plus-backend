package com.example.template.snack;

import java.util.List;

public record SnackDto(
    String id,
    String name,
    String description,
    String category,
    Double price,
    String image,
    String status,
    List<SnackOptionDto> options
) {
}
