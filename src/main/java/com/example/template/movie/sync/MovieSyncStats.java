package com.example.template.movie.sync;

public record MovieSyncStats(
    int fetched,
    int upserted,
    int deactivated,
    int screeningsCreated,
    int screeningsCancelled
) {
}
