package com.example.template.movie.sync;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

record TmdbMoviePage(
    Integer page,
    List<TmdbMovieSummary> results,
    @JsonProperty("total_pages") Integer totalPages
) {
}
