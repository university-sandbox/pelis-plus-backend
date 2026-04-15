package com.example.template.movie;

import java.util.List;

public record MovieListResponse(
    int page,
    List<MovieDto> results,
    int totalPages,
    int totalResults
) {
}
